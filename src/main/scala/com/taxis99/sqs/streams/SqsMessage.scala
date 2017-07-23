package com.taxis99.sqs.streams

import akka.NotUsed
import akka.stream.alpakka.sqs.{Ack, MessageAction, RequeueWithDelay}
import akka.stream.scaladsl.{Flow, Partition}
import com.amazonaws.services.sqs.model.Message
import play.api.libs.json.JsValue

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object SqsMessage {
  
  def autoAck = Flow[Message] map { message =>
    (message, Ack())
  }

  def retryConstraint(maxRetries: Int) = Partition[Message](2, message => {
    val count = message.getAttributes.asScala.get("ApproximateReceiveCount").map(_.toInt).getOrElse(1)
    if (count > maxRetries) 1 else 0
  })

  /**
    * 
    * @param block
    * @return
    */
  def ackOrRetry[A](block: JsValue => Future[A])(implicit ec: ExecutionContext): Flow[JsValue, MessageAction, NotUsed] =
    Flow[JsValue].mapAsync(10) { value =>
      block(value) map (_ => Ack())
    }

  /**
    * 
    * @param block
    * @return
    */
  def ackOrRequeue[A](block: JsValue => Future[A])(implicit ec: ExecutionContext): Flow[JsValue, MessageAction, NotUsed] =
    Flow[JsValue].mapAsync(10) { value =>
      block(value) map (_ => Ack()) recover {
        case _: Throwable => RequeueWithDelay(5)
      }
    }

  def ignoreOrNot = ???
}
