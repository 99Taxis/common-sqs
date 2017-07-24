package com.taxis99.sqs.streams

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.sqs.{Ack, MessageAction, RequeueWithDelay}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Partition, Sink, Source, Zip}
import com.amazonaws.services.sqs.model.Message
import play.api.libs.json.JsValue

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Consumer {

  val LEVEL_OF_PARALLELISM = 10

  /**
    * Returns a consumer flow graph.
    * @param block The message execution block
    * @return A flow graph stage
    */
  def apply[A](delay: Duration, maxRetries: Int = 200)
              (block: JsValue => Future[A])
              (implicit ec: ExecutionContext): Flow[Message, (Message, MessageAction), NotUsed] = {

    val failStrategy = delay match {
      case Duration.Zero | Duration.Inf | Duration.MinusInf | Duration.Undefined =>
        Consumer.ackOrRetry(block)
      case _ =>
        Consumer.ackOrRequeue(delay)(block)
    }

    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val filter = b.add(Flow[Message] filterNot maxRetriesReached(maxRetries))
      val bcast = b.add(Broadcast[Message](2))
      val merge = b.add(Zip[Message, MessageAction])

      // Max retries not reached
      filter ~> bcast
      bcast.out(0) ~>                                      merge.in0
      bcast.out(1) ~> Serializer.decode ~> failStrategy ~> merge.in1
                                                           merge.out

      FlowShape(filter.in, merge.out)
    })
  }

  /**
    * Split the incoming message into two partitions based on the approximate receive count attribute.
    * @param maxRetries The amount of times that this message is allowed to retry
    * @return A message Partition stage
    */
  private def maxRetriesReached(maxRetries: Int)(message: Message): Boolean = {
    val count = message.getAttributes.asScala.get("ApproximateReceiveCount").map(_.toInt).getOrElse(1)
    if (count < maxRetries) false else true
  }

  /**
    * Assign an Ack to the given message, allowing to be piped to the SqsAckSink.
    * @return A flow stage that responds the message with an Ack action
    */
  def ack = Flow[Message] map { message =>
    (message, Ack())
  }

  /**
    * Asynchronously execute the block and if the it succeeds returns an Ack to remove it from the queue, otherwise
    * let AWS resend it again according to its policy. 
    * @param block The execution block
    * @return A flow stage that returns a MessageAction
    */
  def ackOrRetry[A](block: JsValue => Future[A])
                   (implicit ec: ExecutionContext): Flow[JsValue, MessageAction, NotUsed] =
    Flow[JsValue].mapAsync(LEVEL_OF_PARALLELISM) { value =>
      block(value) map (_ => Ack())
    }

  /**
    * Asynchronously execute the block and if the it succeeds returns an Ack to remove it from the queue, otherwise
    * reschedule processing of the message to the given delay.
    * @param block The execution block
    * @return A flow stage that returns a MessageAction
    */
  def ackOrRequeue[A](delay: Duration = 5.minutes)
                     (block: JsValue => Future[A])
                     (implicit ec: ExecutionContext): Flow[JsValue, MessageAction, NotUsed] =
    Flow[JsValue].mapAsync(LEVEL_OF_PARALLELISM) { value =>
      block(value) map (_ => Ack()) recover {
        case _: Throwable => RequeueWithDelay(delay.toSeconds.toInt)
      }
    }
}
