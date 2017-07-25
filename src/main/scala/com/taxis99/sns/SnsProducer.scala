package com.taxis99.sns

import com.taxis99.sqs.{SqsClient, SqsQueue}

import scala.concurrent.{ExecutionContext, Future}

trait SnsProducer[T] {
  implicit def ec: ExecutionContext
  implicit def sns: SqsClient

//  lazy val queueTopic: Future[String] = sns.

  def topic: String
}
