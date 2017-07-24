package com.taxis99.sqs

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.amazonaws.services.sqs.AmazonSQSAsync
import org.elasticmq.rest.sqs.SQSRestServerBuilder
import test.BaseSpec

class SqsClientFactorySpec extends BaseSpec {
  "#default" should "return an AmazonSQSAsync connection class" in {
    val conn = SqsClientFactory.default()
    conn shouldBe a [AmazonSQSAsync]
  }

  "#atLocalhost" should "return AmazonSQSAsync connection to localhost endpoint" in {
    val server = SQSRestServerBuilder.withPort(9325).withInterface("localhost").start()
    server.waitUntilStarted()
    val conn = SqsClientFactory.atLocalhost(9325)
    val q = conn.createQueueAsync("foo").get()
    q.getQueueUrl shouldBe "http://localhost:9325/queue/foo"
    server.stopAndWait()
  }

  "#inMemory" should "start an in memory ElasticMQ and return a AmazonSQSAsync connected to it" in {
    val system = ActorSystem("inMemoryTest")
    val (server, conn) = SqsClientFactory.inMemory(system)
    val q = conn.createQueueAsync("foo").get()
    q.getQueueUrl should endWith ("/queue/foo")
    server.stopAndWait()
    TestKit.shutdownActorSystem(system)
  }
}
