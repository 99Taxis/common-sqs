package com.taxis99.amazon.sqs

import akka.testkit.TestProbe
import com.taxis99.amazon.streams.Serializer
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import it.IntegrationSpec
import it.mocks.{TestConsumer, TestProducer, TestType}
import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class SqsIntegrationSpec extends IntegrationSpec {

  val queueName = "integration-test-q"
  val config = ConfigFactory
    .empty()
    .withValue("sqs", ConfigValueFactory.fromMap(Map(
      queueName -> queueName
    ).asJava))
  aws.createQueueAsync(queueName).get().getQueueUrl

  implicit val sqs = new SqsClient(config)

  val probe = TestProbe()

  val msg = TestType("bar", 100)
  val packedMessage = Serializer.pack(Json.toJson(msg))

  val consumer = new TestConsumer(queueName, probe.ref)
  val producer = new TestProducer(queueName)

  it should "consume the message produce by the producer to the queue" in {
    producer.produce(msg) map { _ =>
      probe expectMsg (10.seconds, msg)
      succeed
    }
  }
}
