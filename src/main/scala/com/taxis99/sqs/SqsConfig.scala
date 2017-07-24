package com.taxis99.sqs

import scala.concurrent.{ExecutionContext, Future}

private[this] trait SqsConfig {
  implicit def ec: ExecutionContext
  implicit def sqs: SqsClient

  lazy val queueUrl: Future[SqsQueue] = sqs.getQueue(name)

  def name: String
}
