package com.taxis99.amazon.sqs

import com.taxis99.amazon.serializers.{ISerializer, PlayJson}

import scala.concurrent.{ExecutionContext, Future}

protected[this] trait SqsConfig {
  implicit def ec: ExecutionContext
  implicit def sqs: SqsClient

  protected lazy val queueConfig: Future[SqsQueue] = sqs.getQueue(queue)

  /**
    * Defines the serialization method to produce/consume messages.
    * @return The serialization object
    */
  def serializer: ISerializer = PlayJson

  /**
    * The queue name in the configuration file
    */
  def queue: String
}
