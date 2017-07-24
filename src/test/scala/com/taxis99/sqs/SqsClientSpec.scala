package com.taxis99.sqs

import akka.testkit.TestProbe
import com.taxis99.sqs.streams.Serializer
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.BeforeAndAfter
import play.api.libs.json.Json
import test.StreamSpec

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SqsClientSpec extends StreamSpec with BeforeAndAfter {

  val sqsConfig = Map("test-q" -> "test-q-DEV")
  val config = ConfigFactory.empty()
    .withValue("sqs", ConfigValueFactory.fromMap(sqsConfig.asJava))

  "#consumer" should "consume messages from the queue" in withInMemoryQ { implicit aws =>
    val sqs = new SqsClient(config)
    val qUrl = aws.createQueueAsync("consumer-q-TEST").get().getQueueUrl
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
    val qUrl = aws.createQueueAsync("producer-q-TEST").get().getQueueUrl
    val sqsQ = SqsQueue("producer-q", "producer-q-TEST", qUrl)

    val produce = sqs.producer(Future.successful(sqsQ))
    
    val msg = Json.obj("foo" -> "bar")
    produce(msg)

//    aws.sendMessageAsync(qUrl, msg.toString()).get()

    val eventualMsgs: Future[Seq[String]] = Future {
      aws.receiveMessageAsync(qUrl).get().getMessages.asScala.map(_.getBody)
    }

    val msgs: Seq[String] = Await.result(eventualMsgs, 3.seconds)
    msgs should contain (msg.toString())
  }
}
