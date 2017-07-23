package com.taxis99.sqs.streams

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow}
import com.amazonaws.services.sqs.model.Message
import play.api.libs.json.{JsValue, Json}

object Serializer {

  /**
    * Compress a JSON it with message pack and return an string representation of the result
    * @return The byte array string
    */
  def encode: Flow[JsValue, String, NotUsed] = Flow[JsValue] map { value =>
    value.toString()
  }

  /**
    * 
    * @return
    */
  def decode: Flow[Message, JsValue, NotUsed] = Flow[Message] map { value =>
    Json.parse(value.getBody)
  }
}
