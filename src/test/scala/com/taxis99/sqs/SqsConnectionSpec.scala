package com.taxis99.sqs

import com.amazonaws.services.sqs.AmazonSQSAsync
import org.elasticmq.rest.sqs.SQSRestServerBuilder
import test.BaseSpec

class SqsConnectionSpec extends BaseSpec {
  "#build" should "return an AmazonSQSAsync connection class" in {
    val conn = SqsConnection.build()
    conn shouldBe a [AmazonSQSAsync]
  }

  "#atLocalhost" should "return AmazonSQSAsync connection to localhost endpoint" in {
    val server = SQSRestServerBuilder.withPort(9325).withInterface("localhost").start()
    server.waitUntilStarted()
    val conn = SqsConnection.atLocalhost(9325)
    val q = conn.createQueueAsync("foo").get()
    q.getQueueUrl shouldBe "http://localhost:9325/queue/foo"
    server.stopAndWait()
  }

  "#inMemory" should "start an in memory ElasticMQ and return a AmazonSQSAsync connected to it" in {
    val (server, conn) = SqsConnection.inMemory()
    val q = conn.createQueueAsync("foo").get()
    q.getQueueUrl should endWith ("/queue/foo")
    server.stopAndWait()
  }
}
