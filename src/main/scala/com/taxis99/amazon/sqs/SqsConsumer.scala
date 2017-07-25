package com.taxis99.amazon.sqs

import play.api.libs.json._

import scala.concurrent.Future

trait SqsConsumer[T] extends SqsConfig {

  /**
    * The consumer function that handles a incoming message from the queue.
    * @param message The incoming message
    * @return A future with the result of this process
    */
  def consume(message: T): Future[Any]

  /**
    * Cast the Json message to the expected type of the consume function.
    * @param message The JsonValue
    * @return A future completed when the casting is done
    */
  protected def onPull(message: JsValue)(implicit fjs: Reads[T]): Future[T] = {
    Json.fromJson[T](message)(fjs) match {
      case JsSuccess(x, _) =>
        Future.successful(x)
      case JsError(_) => Future.failed {
        new ClassCastException(s"Could not parse message $message")
      }
    }
  }

  /**
    * Starts to receive message into the consume function.
    * @return A future completed when the consumer stopped
    */
  def startConsumer()(implicit fjs: Reads[T]) = sqs.consumer(queueConfig)(onPull(_) flatMap consume)
}
