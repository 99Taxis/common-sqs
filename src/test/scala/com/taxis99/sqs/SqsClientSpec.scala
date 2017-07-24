package com.taxis99.sqs

import akka.testkit.TestProbe
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import play.api.libs.json.Json
import test.StreamSpec

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

class SqsClientSpec extends StreamSpec {

  implicit val (server, aws) = SqsClientBuilder.inMemory()

  val sqsConfig = Map("test-q" -> "test-q-DEV")
  val config = ConfigFactory.empty()
    .withValue("sqs", ConfigValueFactory.fromMap(sqsConfig.asJava))


  override def afterAll = {
    server.stopAndWait()
    super.afterAll
  }

  "#consumer(queueConfig)" should "consume messages from the queue" in {
    val sqs = new SqsClient(config)
    val qUrl = aws.createQueueAsync("consumer-q-TEST").get().getQueueUrl
    val sqsQ = SqsQueue("consumer-q", "consumer-q-TEST", qUrl)

    val msg = Json.obj("foo" -> "bar")
    val probe = TestProbe()
    aws.sendMessageAsync(qUrl, msg.toString()).get()

    sqs.consumer(Future.successful(sqsQ)) { value =>
      Future.successful(probe.ref ! value)
    }

    probe expectMsg msg
  }
  
  "#producer(queueConfig)" should "produce message to the queue" ignore {
    val sqs = new SqsClient(config)
    val qUrl = aws.createQueueAsync("consumer-q-TEST").get().getQueueUrl
    val sqsQ = SqsQueue("consumer-q", "consumer-q-TEST", qUrl)
    val msg = Json.obj("foo" -> "bar")

    val produce = sqs.producer(Future.successful(sqsQ))

    Await.ready(produce(msg), 5.seconds)
    aws.receiveMessage(qUrl).getMessages.asScala.map(_.getBody) should contain (msg.toString())
    aws.receiveMessageAsync(qUrl).get().getMessages.asScala.map(_.getBody) should contain (msg.toString())
  }
}
