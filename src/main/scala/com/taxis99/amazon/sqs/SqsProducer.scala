package com.taxis99.amazon.sqs

import akka.Done
import akka.stream.QueueOfferResult
import play.api.libs.json.{Json, Writes}

import scala.concurrent.{Future, Promise}

trait SqsProducer[T] extends SqsConfig {

  private lazy val producer = sqs.producer(queueConfig)

  /**
    * Produces a new message to the queue. The message must be serializable to Json.
    * @param message The message to be sent
    * @return A future completed when the message was sent
    */
  def produce(message: T)(implicit tjs: Writes[T]): Future[Done] = {
    val done = Promise[Done]
    producer flatMap { queue =>
      queue.offer(Json.toJson(message) -> done) flatMap {
        case QueueOfferResult.Enqueued => done.future
        case r: QueueOfferResult => Future.failed(new Exception(s"Could not enqueue $r"))
      }
    }
  }
}
