package com.taxis99.amazon.serializers

import play.api.libs.json.JsValue

trait ISerializer {
  def encode(value: JsValue): String

  def decode(value: String): JsValue
}
