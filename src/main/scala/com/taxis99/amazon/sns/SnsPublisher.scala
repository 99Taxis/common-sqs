package com.taxis99.amazon.sns

import akka.Done
import akka.stream.QueueOfferResult
import com.taxis99.amazon.serializers.{ISerializer, PlayJson}
import play.api.libs.json.{Json, Writes}

import scala.concurrent.{ExecutionContext, Future, Promise}

trait SnsPublisher[T] {
  implicit def ec: ExecutionContext
  implicit def sns: SnsClient

  protected lazy val topicConfig: Future[SnsTopic] = sns.getTopic(topic)

  private lazy val publisher = sns.publisher(topicConfig)

  /**
    * Defines the serialization method to produce messages.
    * @return The serialization object
    */
  implicit def serializer: ISerializer = PlayJson

  /**
    * The topic name in the configuration file
    */
  def topic: String

  /**
    * Publishes a new message to the topic. The message must be serializable to Json.
    * @param message The message to be sent
    * @return A future completed when the message was sent
    */
  def publish(message: T)(implicit tjs: Writes[T]): Future[Done] = {
    val done = Promise[Done]
    publisher flatMap { queue =>
      queue.offer(Json.toJson(message) -> done) flatMap {
        case QueueOfferResult.Enqueued => done.future
        case r: QueueOfferResult => Future.failed(new Exception(s"Could not enqueue $r"))
      }
    }
  }
}
