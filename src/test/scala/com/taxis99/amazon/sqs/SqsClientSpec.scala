package com.taxis99.amazon.sqs

import akka.Done
import akka.testkit.TestProbe
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.taxis99.amazon.serializers.{ISerializer, PlayJson}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import play.api.libs.json.Json
import test.StreamSpec

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}

class SqsClientSpec extends StreamSpec {
  implicit val serializer: ISerializer = PlayJson

  val (queueKey, queueName) = ("test-q", "test-q-DEV")
  val config: Config = ConfigFactory.parseMap(Map[String, String](
    s"sqs.$queueKey" -> queueName,
    s"sqs.settings.default.waitTimeSeconds" -> "20",
    s"sqs.settings.default.maxBufferSize" -> "100",
    s"sqs.settings.default.maxBatchSize" -> "10",
    s"sqs.settings.default.maxRetries" -> "200"
  ).asJava)

  println(config)

  def createQueue(queueName: String)(implicit aws: AmazonSQSAsync): String = {
    aws.createQueue(queueName).getQueueUrl
  }

  "#getQueue" should "return an SqsQueue config object eventually" in withInMemoryQueue { implicit aws =>
    createQueue("test-q-DEV")
    val sqs = new SqsClient(config)
    val q = sqs.getQueue(queueKey)
    
    q map { case SqsQueue(key, name, url) =>
      key shouldBe queueKey
      name shouldBe queueName
      url should endWith (s"/queue/$queueName")
    }
  }

  "#consumer" should "consume messages from the queue" in withInMemoryQueue { implicit aws =>
    val sqs = new SqsClient(config)
    val qUrl = createQueue("consumer-q-TEST")
    val sqsQ = SqsQueue("consumer-q", "consumer-q-TEST", qUrl)

    val jsonMsg = Json.obj("foo" -> "bar")
    val probe = TestProbe()

    // Produce some message
    aws.sendMessage(qUrl, jsonMsg.toString())

    sqs.consumer(Future.successful(sqsQ)) { value =>
      Future.successful(probe.ref ! value)
    }

    probe expectMsg jsonMsg
    succeed
  }
  
  "#producer" should "return a internal queue that delivers messages to SQS fulfilling a promise when it is done" in withInMemoryQueue { implicit aws =>
    val sqs = new SqsClient(config)
    val qUrl = createQueue("producer-q-TEST")
    val sqsQ = SqsQueue("producer-q", "producer-q-TEST", qUrl)

    val produce = sqs.producer(Future.successful(sqsQ))
    val msg = Json.obj("foo" -> "bar")

    produce.flatMap { queue =>
      val done = Promise[Done]
      queue.offer(msg -> done)
      done.future
    } map { _ =>
      val msgs = aws.receiveMessage(qUrl).getMessages.asScala.map(_.getBody)
      msgs should contain (msg.toString())
    }
  }
}
