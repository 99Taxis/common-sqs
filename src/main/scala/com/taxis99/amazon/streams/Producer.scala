package com.taxis99.amazon.streams

import akka.stream.alpakka.sqs.scaladsl.Result
import akka.stream.scaladsl._
import akka.stream.{OverflowStrategy, SinkShape}
import akka.{Done, NotUsed}
import com.amazonaws.services.sns.model.PublishResult
import com.taxis99.amazon.serializers.ISerializer
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import scala.concurrent.{Future, Promise}

object Producer {

  protected val LevelOfParallelism = 50
  
  protected val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  sealed abstract class ActionResult
  case object Ok extends ActionResult
  case object Nok extends ActionResult

  /**
    * Create a producer sink stage that receives a message and a promise and fulfil the promise weather the message was
    * produced correctly to AWS
    * @param sender
    * @return
    */
  def sqs(sender: Flow[String, Result, NotUsed])
         (implicit serializer: ISerializer): Sink[(JsValue, Promise[Done]), NotUsed] =
  Sink.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val src = builder.add(Unzip[JsValue, Promise[Done]])

    val send = builder.add(SqsFlowWrapper(sender))

    val merge = builder.add(Zip[ActionResult, Promise[Done]])

    val finish = builder.add(FulfilPromise)

    src.out0 ~> send ~> merge.in0
    src.out1         ~> merge.in1
                        merge.out ~> finish

    SinkShape(src.in)
  })

  /**
    * Create a publish sink stage that receives a message and a promise and fulfil the promise weather the message was
    * publish correctly to AWS
    * @param sender
    * @param serializer
    * @return
    */
  def sns(sender: Flow[String, PublishResult, NotUsed])
         (implicit serializer: ISerializer) = Sink.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val src = builder.add(Unzip[JsValue, Promise[Done]])

    val send = builder.add(SnsFlowWrapper(sender))

    val merge = builder.add(Zip[ActionResult, Promise[Done]])

    val finish = builder.add(FulfilPromise)

    src.out0 ~> send ~> merge.in0
    src.out1         ~> merge.in1
                        merge.out ~> finish

    SinkShape(src.in)
  })

  protected def SqsFlowWrapper(sqsFlow: Flow[String, Result, NotUsed])
                              (implicit serializer: ISerializer): Flow[JsValue, ActionResult, NotUsed] =
    Flow[JsValue].mapAsync(LevelOfParallelism)(message => Future.fromTry(Serializer.encode(message)))
      .via(sqsFlow map { result =>
        val id = result.metadata.getMessageId
        logger.debug(s"Producing message $id")
        Ok
      })
      .recover({ case e: Exception =>
        logger.error("Could not produce message", e)
        Nok
      })

  protected def SnsFlowWrapper(snsFlow: Flow[String, PublishResult, NotUsed])
                              (implicit serializer: ISerializer): Flow[JsValue, ActionResult, NotUsed] =
    Flow[JsValue].mapAsync(LevelOfParallelism)(message => Future.fromTry(Serializer.encode(message)))
      .via(snsFlow map { result =>
        val id = result.getMessageId
        logger.debug(s"Publishing message $id")
        Ok
      })
      .recover({ case e: Exception =>
        logger.error("Could not produce message", e)
        Nok
      })

  protected def FulfilPromise: Sink[(ActionResult, Promise[Done]), Future[Done]] =
    Sink.foreach[(ActionResult, Promise[Done])] { case (result, promise) =>
      result match {
        case Ok => promise.success(Done)
        case Nok => promise.failure(new Exception("Could not send message, see logs for more info"))
      }
    }
}
