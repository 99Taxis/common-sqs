package com.taxis99.sqs.streams

import akka.pattern.pipe
import akka.stream.ClosedShape
import akka.stream.alpakka.sqs.{Ack, RequeueWithDelay}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import akka.testkit.TestProbe
import com.amazonaws.services.sqs.model.Message
import org.scalatest.RecoverMethods
import play.api.libs.json.{JsValue, Json}
import test.StreamSpec

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ConsumerSpec extends StreamSpec {
  "#apply" should "send an Ack and delete the message from queue if the block fn succeeds" in {
    val msg = new Message()
    msg.setBody("{}")

    val fn = (_: JsValue) => Future.successful("ok")
    val probe = TestProbe()

    Source.single(msg) via Consumer(Duration.Zero)(fn) runWith Sink.head pipeTo probe.ref
    probe expectMsg ((msg, Ack()))
  }

  it should "not delete message from queue if the block fn fails" in {
    val msg = new Message()
    msg.setBody("{}")

    val fn = (_: JsValue) => Future.failed(new Exception("nok"))
    val probe = TestProbe()

    recoverToSucceededIf[Exception] {
      Source.single(msg) via Consumer(Duration.Zero)(fn) runWith Sink.headOption pipeTo probe.ref
    }
  }

  it should "not delete message from queue if the deserialization fails" in {
    val msg = new Message()
    msg.setBody("{not a valid JSON}")

    val fn = (_: JsValue) => Future.failed(new Exception("nok"))
    val probe = TestProbe()

    recoverToSucceededIf[Exception] {
      Source.single(msg) via Consumer(Duration.Zero)(fn) runWith Sink.headOption pipeTo probe.ref
    }
  }

  it should "send an Ack and delete the message from queue if the block fn succeeds with requeue activated" in {
    val msg = new Message()
    msg.setBody("{}")

    val fn = (_: JsValue) => Future.successful("ok")
    val probe = TestProbe()
    
    Source.single(msg) via Consumer(1.minute)(fn) runWith Sink.head pipeTo probe.ref
    probe expectMsg ((msg, Ack()))
  }

  it should "requeue the message with the given delay if block fn fails" in {
    val msg = new Message()
    msg.setBody("""{"foo":"bar"}""")

    val fn = (_: JsValue) => Future.failed(new Exception("nok"))
    val probe = TestProbe()

    Source.single(msg) via Consumer(1.minute)(fn) runWith Sink.head pipeTo probe.ref
    probe expectMsg ((msg, RequeueWithDelay(60)))
  }

  it should "discard a message if it achieved max retries limit" in {
    val msg = new Message()
    msg.setAttributes(Map("ApproximateReceiveCount" -> "350").asJava)

    val fn = (_: JsValue) => Future.successful("ok")
    val probe = TestProbe()

    Source.single(msg) via Consumer(Duration.Zero, 350)(fn) runWith Sink.headOption pipeTo probe.ref
    probe expectMsg None
  }

  "#ack" should "return the message with an Ack action" in {
    val msg = new Message()
    val probe = TestProbe()
    Source.single(msg) via Consumer.ack runWith Sink.head pipeTo probe.ref
    probe expectMsg ((msg, Ack()))
  }

  "#ackOrRetry" should "return an Ack if future block succeeded" in {
    val msg = Json.obj()
    val fn = (_: JsValue) => Future.successful("ok")

    val probe = TestProbe()
    Source.single(msg) via Consumer.ackOrRetry(fn) runWith Sink.actorRef(probe.ref, "ok")
    probe expectMsg Ack()
  }

  "#ackOrRequeue" should "return an Ack if future block succeeded" in {
    val msg = Json.obj()
    val fn = (_: JsValue) => Future.successful("ok")

    val probe = TestProbe()
    Source.single(msg) via Consumer.ackOrRequeue(1.second)(fn) runWith Sink.actorRef(probe.ref, "ok")
    probe expectMsg Ack()
  }

  it should "return an RequeueWithDelay if future block fails" in {
    val delay = 1.second
    val msg = Json.obj()
    val fn = (_: JsValue) => Future.failed(new Exception("nok"))

    val probe = TestProbe()

    Source.single(msg) via Consumer.ackOrRequeue(delay)(fn) runWith Sink.actorRef(probe.ref, "ok")
    probe expectMsg RequeueWithDelay(1)
  }
}
