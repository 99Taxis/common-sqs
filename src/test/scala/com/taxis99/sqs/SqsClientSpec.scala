package com.taxis99.sqs

import akka.testkit.TestProbe
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.taxis99.streams.Serializer
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.concurrent.ScalaFutures._
import play.api.libs.json.Json
import test.StreamSpec

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SqsClientSpec extends StreamSpec {
  val config = ConfigFactory.empty()

  def createQueue(queueName: String)(implicit aws: AmazonSQSAsync): String = {
    aws.createQueueAsync(queueName).get().getQueueUrl
  }

  "#getQueue" should "" in withInMemoryQ { implicit aws =>
    val (configKey, configName) = ("test-q", "test-q-DEV")
    createQueue("test-q-DEV")
    val sqs = new SqsClient(config
      .withValue("sqs", ConfigValueFactory.fromMap(Map(
        configKey -> configName
      ).asJava)))
    val q = sqs.getQueue(configKey)

    whenReady(q) { case SqsQueue(key, name, url) =>
        key shouldBe configKey
        name shouldBe configName
        url should endWith (s"/queue/$configName")
    }
  }

  "#consumer" should "consume messages from the queue" in withInMemoryQ { implicit aws =>
    val sqs = new SqsClient(config)
    val qUrl = createQueue("consumer-q-TEST")
    val sqsQ = SqsQueue("consumer-q", "consumer-q-TEST", qUrl)

    val unpackedMsg = Json.obj("foo" -> "bar")
    val packedMsg = Serializer.pack(unpackedMsg)
    val probe = TestProbe()
    aws.sendMessageAsync(qUrl, packedMsg).get()

    sqs.consumer(Future.successful(sqsQ)) { value =>
      Future.successful(probe.ref ! value)
    }

    probe expectMsg unpackedMsg
  }
  
  "#producer" should "produce message to the queue" in withInMemoryQ { implicit aws =>
    val sqs = new SqsClient(config)
    val qUrl = createQueue("producer-q-TEST")
    val sqsQ = SqsQueue("producer-q", "producer-q-TEST", qUrl)

    val produce = sqs.producer(Future.successful(sqsQ))
    
    val msg = Json.obj("foo" -> "bar")
    produce(msg)

    // Don't know why this is required to the message to appear on the
    aws.sendMessageAsync(qUrl, msg.toString()).get()

    val eventualMsgs: Future[Seq[String]] = Future {
      aws.receiveMessageAsync(qUrl).get().getMessages.asScala.map(_.getBody)
    }

    whenReady(eventualMsgs) { msgs =>
      msgs should contain (Serializer.pack(msg))
    }
  }
}
