package com.taxis99.amazon.sqs

import scala.concurrent.{ExecutionContext, Future}

protected[this] trait SqsConfig {
  implicit def ec: ExecutionContext
  implicit def sqs: SqsClient

  protected lazy val queueConfig: Future[SqsQueue] = sqs.getQueue(queue)

  def queue: String
}
