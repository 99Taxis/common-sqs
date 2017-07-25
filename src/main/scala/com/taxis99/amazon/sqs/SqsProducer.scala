package com.taxis99.amazon.sqs

import akka.Done
import play.api.libs.json.{Json, Writes}

import scala.concurrent.Future

trait SqsProducer[T] extends SqsConfig {

  /**
    * Produces a new message to the queue. The message must be serializable to Json.
    * @param message The message to be sent
    * @return A future completed when the message was sent
    */
  def produce(message: T)(implicit tjs: Writes[T]): Future[Done] = {
    sqs.producer(queueConfig)(Json.toJson(message))
  }
}
