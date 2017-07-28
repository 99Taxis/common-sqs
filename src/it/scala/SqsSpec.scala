package com.taxis99.amazon.sqs

import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import it.IntegrationSpec
import it.mocks.{TestConsumer, TestProducer, TestType}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class SqsSpec extends IntegrationSpec {

  val queueName = "integration-test-q"
  val config = ConfigFactory.parseMap(Map[String, String](
      s"sqs.$queueName" -> queueName
    ).asJava)

  implicit val sqs = new SqsClient(config)

  val probe = TestProbe()
  val consumer = new TestConsumer(queueName, probe.ref)
  val producer = new TestProducer(queueName)

  it should "consume the message produce by the producer to the queue" in {
    val msg = TestType("bar", 100)
    
    producer.produce(msg) map { _ =>
      probe expectMsg (10.seconds, msg)
      succeed
    }
  }
}
