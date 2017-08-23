package com.taxis99.amazon.sqs

import akka.testkit.TestProbe
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.model.{SendMessageRequest, SendMessageResult}
import com.typesafe.config.ConfigFactory
import it.IntegrationSpec
import it.mocks.{TestConsumer, TestProducer, TestType}

import scala.collection.JavaConverters._
import scala.concurrent.Promise
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

  it should "keep consuming even after a consumer exception" in {
    val msgEx = TestType("fail", -1)
    val msg1 = TestType("ok1", 1)
    val msg2 = TestType("ok2", 2)
    val msg3 = TestType("ok3", 3)

    producer.produce(msg1)
      .flatMap(_ => producer.produce(msgEx))
      .flatMap(_ => producer.produce(msg2))
      .flatMap(_ => producer.produce(msg3)).map { _ =>
      probe expectMsgAllOf (msg1, msg2, msg3)
      succeed
    }
  }

  it should "keep consuming even after a decode exception" in {
    val msgEx = produceRawMessage(queueName, "{bad formated message}")
    val msg1 = TestType("ok1", 1)
    val msg2 = TestType("ok2", 2)
    val msg3 = TestType("ok3", 3)

    msgEx
      .flatMap(_ => producer.produce(msg1))
      .flatMap(_ => producer.produce(msg2))
      .flatMap(_ => producer.produce(msg3))
      .map { _ =>
      probe expectMsgAllOf (msg1, msg2, msg3)
      succeed
    }
  }

  it should "publish several messages" in {
    val msg1 = TestType("1", 1)
    val msg2 = TestType("2", 2)
    val msg3 = TestType("3", 3)

    for {
      _ <- producer.produce(msg1)
      _ <- producer.produce(msg2)
      _ <- producer.produce(msg3)
    } yield {
      probe expectMsgAllOf (msg1, msg2, msg3)
      succeed
    }
  }
}
