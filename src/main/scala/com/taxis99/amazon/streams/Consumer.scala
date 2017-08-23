package com.taxis99.amazon.streams

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.sqs.{ChangeMessageVisibility, Delete, Ignore, MessageAction}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Zip}
import com.amazonaws.services.sqs.model.Message
import com.taxis99.amazon.serializers.ISerializer
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object Consumer {

  protected val LevelOfParallelism = 50

  protected val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  /**
    * Returns a consumer flow graph.
    * @param block The message execution block
    * @return A flow graph stage
    */
  def apply[A](delay: Duration, maxRetries: Int = 200)(block: JsValue => Future[A])
              (implicit ec: ExecutionContext, serializer: ISerializer): Flow[Message, (Message, MessageAction), NotUsed] = {

    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val filter = builder.add(Flow[Message] filterNot maxRetriesReached(maxRetries))
      val bcast = builder.add(Broadcast[Message](2))
      val consume = builder.add(delay match {
        case Duration.Zero | Duration.Inf | Duration.MinusInf | Duration.Undefined =>
          Consumer.ackOrRetry(block)
        case _ =>
          Consumer.ackOrRequeue(delay)(block)
      })
      val merge = builder.add(Zip[Message, MessageAction])

      // Max retries not reached
      filter ~> bcast
      bcast.out(0) ~>            merge.in0
      bcast.out(1) ~> consume ~> merge.in1

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
    count >= maxRetries
  }

  /**
    * Asynchronously execute the block and if the it succeeds returns an Ack to remove it from the queue, otherwise
    * let AWS resend it again according to its policy.
    * @param block The execution block
    * @return A flow stage that returns a MessageAction
    */
  private def ackOrRetry[A](block: JsValue => Future[A])
                   (implicit ec: ExecutionContext, serializer: ISerializer): Flow[Message, MessageAction, NotUsed] =
    Flow[Message].mapAsync(LevelOfParallelism) { message =>
      logger.debug(s"Consuming message $message")

      Future.fromTry(Serializer.decode(message)) flatMap block map {
        case ChangeMessageVisibility(visibilityTimeout) => ChangeMessageVisibility(visibilityTimeout)
        case _ => Delete()
      } recover {
        case e =>
          logger.error(s"Could not consume message $message", e)
          Ignore()
      }
    }

   /**
     * Asynchronously execute the block and if the it succeeds returns an Ack to remove it from the queue, otherwise
     * reschedule processing of the message to the given delay.
     * @param block The execution block
     * @return A flow stage that returns a MessageAction
     */
   private def ackOrRequeue[A](delay: Duration = 5.minutes)(block: JsValue => Future[A])
                      (implicit ec: ExecutionContext, serializer: ISerializer): Flow[Message, MessageAction, NotUsed] = {
     val seconds = delay.toSeconds.toInt
     Flow[Message].mapAsync(LevelOfParallelism) { message =>
       logger.debug(s"Consuming message $message")
       Future.fromTry(Serializer.decode(message)) flatMap block map (_ => Delete()) recover {
         case e => {
           logger.error(s"Could not consume message $message retrying in $seconds secs", e)
           ChangeMessageVisibility(seconds)
         }
       }
     }
   }
}
