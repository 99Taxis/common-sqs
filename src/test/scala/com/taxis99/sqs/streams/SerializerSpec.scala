package com.taxis99.sqs.streams

import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestProbe
import com.amazonaws.services.sqs.model.Message
import play.api.libs.json.Json
import test.StreamSpec

class SerializerSpec extends StreamSpec {

  case class MyCustomType(s: String, i: Int, f: Double, b: Boolean, m: Map[String, String], l: Seq[Int])

  object MyCustomType {
    implicit val myCustomTypeFormat = Json.format[MyCustomType]
  }

  val msg = MyCustomType("foo", 1, 1.0, false, Map("1" -> "2"), Seq(0 , 1))
  val jsonMsg = Json.toJson(msg)

  "#econde" should "return the string representation of the JsValue" in {
    val probe = TestProbe()

    Source.single(jsonMsg) via Serializer.encode runWith Sink.actorRef(probe.ref, jsonMsg)
    probe.expectMsg(jsonMsg.toString())
  }

  "#decode" should "return the JsValue from a Amazon SQS message" in {
    val probe = TestProbe()
    
    val sqsMsg = new Message()
    sqsMsg.setBody(jsonMsg.toString())

    Source.single(sqsMsg) via Serializer.decode runWith Sink.actorRef(probe.ref, jsonMsg)
    probe.expectMsg(jsonMsg)
  }
}
