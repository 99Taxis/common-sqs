package com.taxis99.amazon.sns

import akka.Done
import com.taxis99.amazon.serializers.{ISerializer, PlayJson}
import play.api.libs.json.{Json, Writes}

import scala.concurrent.{ExecutionContext, Future}

trait SnsPublisher[T] {
  implicit def ec: ExecutionContext
  implicit def sns: SnsClient

  protected lazy val topicConfig: Future[SnsTopic] = sns.getTopic(topic)

  /**
    * Defines the serialization method to produce messages.
    * @return The serialization object
    */
  def serializer: ISerializer = PlayJson

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
    sns.publisher(topicConfig, serializer)(Json.toJson(message))
  }
}
