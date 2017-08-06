package com.taxis99.amazon.serializers

import play.api.libs.json.Json
import test.BaseSpec

class SerializersSpec extends BaseSpec {

  val msg = Json.obj("str" -> "foo bar", "num" -> 100, "bool" -> true, "double" -> 1.23, "map" -> Map("a" -> "b"),
    "list" -> Seq(1, 2, 4))

  "PlayJson" should "encode and decode a JsValue back and forth" in {
    PlayJson.decode(PlayJson.encode(msg)) shouldBe msg
  }

  "MsgPack" should "encode and decode a JsValue back and forth" in {
    MsgPack.decode(MsgPack.encode(msg)) shouldBe msg
  }
}
