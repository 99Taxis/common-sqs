package com.taxis99.amazon.streams

import com.amazonaws.services.sqs.model.Message
import com.taxis99.amazon.serializers.{MsgPack, PlayJson}
import play.api.libs.json.Json
import test.BaseSpec

import scala.util.Success

class SerializerSpec extends BaseSpec {

  case class MyCustomType(s: String, i: Int, f: Double, b: Boolean, m: Map[String, String], l: Seq[Int])

  object MyCustomType {
    implicit val myCustomTypeFormat = Json.format[MyCustomType]
  }

  val msg = MyCustomType("foo", 1, 1.0, false, Map("1" -> "2"), Seq(0 , 1))
  val jsonMsg = Json.toJson(msg)

  "PlayJson#enconde" should "return the string representation of the JsValue" in {
    Serializer.encode(jsonMsg)(PlayJson) shouldBe Success(jsonMsg.toString())
  }

  "PlayJson#decode" should "return the JsValue from a Amazon SQS message" in {
    val sqsMsg = new Message()
    sqsMsg.setBody(jsonMsg.toString())

    Serializer.decode(sqsMsg)(PlayJson) shouldBe Success(jsonMsg)
  }

  "MsgPack#enconde" should "return the string representation of the JsValue" in {
    Serializer.encode(jsonMsg)(MsgPack) shouldBe Success(MsgPack.encode(jsonMsg))
  }

  "MsgPack#decode" should "return the JsValue from a Amazon SQS message" in {
    val sqsMsg = new Message()
    sqsMsg.setBody(MsgPack.encode(jsonMsg))

    Serializer.decode(sqsMsg)(MsgPack) shouldBe Success(jsonMsg)
  }
}
