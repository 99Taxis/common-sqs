package com.taxis99.sqs

import com.typesafe.config.ConfigFactory
import test.StreamSpec

import scala.concurrent.ExecutionContext.Implicits.global

class SqsClientSpec extends StreamSpec {

  implicit val (server, sqsClient) = SqsClientBuilder.inMemory()

  val config = ConfigFactory.empty()

  val sqs = new SqsClient(config)

  override def afterAll = {
    server.stopAndWait()
    super.afterAll
  }

  "#consumer(queueConfig)" should "" in {
    
  }
  
  "#producer(queueConfig)" should "" in {
    
  }
}
