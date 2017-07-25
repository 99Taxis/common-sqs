package com.taxis99.amazon.sqs

import scala.concurrent.{ExecutionContext, Future}

protected[this] trait SqsConfig {
  implicit def ec: ExecutionContext
  implicit def sqs: SqsClient

  lazy val queueUrl: Future[SqsQueue] = sqs.getQueue(name)

  def name: String
}
