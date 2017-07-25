package com.taxis99.amazon

import org.elasticmq.rest.sqs.{SQSLimits, SQSRestServerBuilder}

object ElasticMQ {

  /**
    * Returns an in memory instance of ElasticMQ
    */
  def inMemory() = SQSRestServerBuilder
    .withDynamicPort()
    .withSQSLimits(SQSLimits.Relaxed)
    .start()
}
