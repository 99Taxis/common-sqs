package com.taxis99.amazon.streams

import akka.stream.alpakka.sqs.{ChangeMessageVisibility, Delete, Ignore}
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.sqs.model.Message
import com.taxis99.amazon.serializers.{ISerializer, PlayJson}
import org.scalatest.concurrent.PatienceConfiguration
import play.api.libs.json.{JsValue, Json}
import test.StreamSpec

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

class ConsumerSpec extends StreamSpec {
  implicit val serializer: ISerializer = PlayJson
  
  val msg = Json.obj("foo" -> "bar")
  val strMsg = msg.toString()

  "#apply" should "send an Ack and delete the message from queue if the block fn succeeds" in {
    val fn = (_: JsValue) => Future.successful("ok")
    val msg = new Message()
    msg.setBody(strMsg)

    Source.single(msg) via Consumer(Duration.Zero)(fn) runWith Sink.head map { result =>
      result shouldBe ((msg, Delete()))
    }
  }

  it should "not delete message from queue if the block fn fails" in {
    val fn = (_: JsValue) => Future.failed(new Exception("nok"))
    val msg = new Message()
    msg.setBody(strMsg)

    Source.single(msg) via Consumer(Duration.Zero)(fn) runWith Sink.head map { result =>
      result shouldBe ((msg, Ignore()))
    }
  }

  it should "not delete message from queue if the deserialization fails" in {
    val fn = (_: JsValue) => Future.failed(new Exception("nok"))
    val msg = new Message()
    msg.setBody("{not a valid JSON}")

    Source.single(msg) via Consumer(Duration.Zero)(fn) runWith Sink.head map { result =>
      result shouldBe ((msg, Ignore()))
    }
  }

  it should "discard a message if it achieved max retries limit" in {
    val fn = (_: JsValue) => Future.successful("ok")
    val msg = new Message()
    msg.setAttributes(Map("ApproximateReceiveCount" -> "350").asJava)

    Source.single(msg) via Consumer(Duration.Zero, 350)(fn) runWith Sink.headOption map { msg =>
      msg shouldBe None
    }
  }

  it should "requeue the message if it future returns an message action" in {
    val requeue = ChangeMessageVisibility(10000)
    val fn = (_: JsValue) => Future.successful(requeue)
    val msg = new Message()
    msg.setBody("{}")

    Source.single(msg) via Consumer(Duration.Zero, 350)(fn) runWith Sink.head map { result =>
      result shouldBe ((msg, requeue))
    }
  }

  it should "send an Ack and delete the message from queue if the block fn succeeds with requeue activated" in {
    val fn = (_: JsValue) => Future.successful("ok")
    val msg = new Message()
    msg.setBody(strMsg)

    Source.single(msg) via Consumer(1.minute)(fn) runWith Sink.head map { result =>
      result shouldBe ((msg, Delete()))
    }
  }

  it should "requeue the message with the given delay if block fn fails" in {
    val fn = (_: JsValue) => Future.failed(new Exception("nok"))
    val msg = new Message()
    msg.setBody(strMsg)

    Source.single(msg) via Consumer(1.minute)(fn) runWith Sink.head map { result =>
      result shouldBe ((msg, ChangeMessageVisibility(60)))
    }
  }
}
