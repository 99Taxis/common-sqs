package com.taxis99.sqs.streams

import akka.stream.ClosedShape
import akka.stream.alpakka.sqs.{Ack, RequeueWithDelay}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import akka.testkit.TestProbe
import com.amazonaws.services.sqs.model.Message
import play.api.libs.json.{JsValue, Json}
import test.StreamSpec

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ConsumerSpec extends StreamSpec {
  "#apply" should "create a consumer graph flow" in {
    // TODO
  }

  "#ack" should "return the message with an Ack action" in {
    val msg = new Message()
    val probe = TestProbe()
    Source.single(msg) via Consumer.ack runWith Sink.actorRef(probe.ref, "ok")
    probe.expectMsg((msg, Ack()))
  }

  "#maxRetriesSplit" should "output message to partition zero if ReceiveCount not exceed max retries and to one if does" in {
    val msgOk = new Message()
    val msgEx = new Message()
    msgEx.setAttributes(Map("ApproximateReceiveCount" -> "350").asJava)

    val ok = TestProbe()
    val exceeded = TestProbe()

    val sinkOk = Sink.actorRef(ok.ref, "ok")
    val sinkEx = Sink.actorRef(exceeded.ref, "ok")

    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val in = Source(Seq(msgOk, msgEx))
      val split = b.add(Consumer.maxRetriesSplit(200))

      in ~> split
            split.out(0) ~> sinkOk
            split.out(1) ~> sinkEx

      ClosedShape
    })

    graph.run()
    ok.expectMsg(msgOk)
    exceeded.expectMsg(msgEx)
  }

  "#ackOrRetry" should "return an Ack if future block succeeded" in {
    val msg = Json.obj()
    val fn = (_: JsValue) => Future.successful("ok")

    val probe = TestProbe()
    Source.single(msg) via Consumer.ackOrRetry(fn) runWith Sink.actorRef(probe.ref, "ok")
    probe.expectMsg(Ack())
  }

  "#ackOrRequeue" should "return an Ack if future block succeeded" in {
    val msg = Json.obj()
    val fn = (_: JsValue) => Future.successful("ok")

    val probe = TestProbe()
    Source.single(msg) via Consumer.ackOrRequeue(1.second)(fn) runWith Sink.actorRef(probe.ref, "ok")
    probe.expectMsg(Ack())
  }

  it should "return an RequeueWithDelay if future block fails" in {
    val delay = 1.second
    val msg = Json.obj()
    val fn = (_: JsValue) => Future.failed(new Exception("nok"))

    val probe = TestProbe()

    Source.single(msg) via Consumer.ackOrRequeue(delay)(fn) runWith Sink.actorRef(probe.ref, "ok")
    probe.expectMsg(RequeueWithDelay(1))
  }
}
