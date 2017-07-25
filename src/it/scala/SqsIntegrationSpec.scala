package com.taxis99.sqs

import akka.testkit.TestProbe
import com.taxis99.sqs.streams.Serializer
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import it.BaseSpec
import mocks.{TestConsumer, TestProducer, TestType}
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class SqsIntegrationSpec extends BaseSpec {

  val queueName = "integration-test-q"
  val config = ConfigFactory
    .empty()
    .withValue("sqs", ConfigValueFactory.fromMap(Map(
      queueName -> queueName
    ).asJava))

  val queueUrl = aws.createQueueAsync(queueName).get().getQueueUrl

  implicit val sqs = new SqsClient(config)

  val probe = TestProbe()

  val msg = TestType("bar", 100)
  val packedMessage = Serializer.pack(Json.toJson(msg))

  val producer = new TestProducer(queueName)
  val consumer = new TestConsumer(queueName, probe.ref)

  it should "consume the message produce by the producer to the queue" in {
    producer.produce(msg).map { _ =>
      probe expectMsg (10.seconds, msg)
      succeed
    }
  }
}
