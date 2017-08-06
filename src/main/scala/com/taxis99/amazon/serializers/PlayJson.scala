package com.taxis99.amazon.serializers

import play.api.libs.json.{JsValue, Json}

object PlayJson extends ISerializer {

  def encode(value: JsValue): String = value.toString()

  def decode(value: String): JsValue = Json.parse(value)
}
