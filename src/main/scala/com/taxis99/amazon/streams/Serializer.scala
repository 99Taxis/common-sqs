package com.taxis99.amazon.streams

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sqs.model.Message
import com.taxis99.amazon.serializers.ISerializer
import play.api.libs.json.JsValue

object Serializer {

  /**
    * Returns a string from a JsValue.
    * @return The byte array string
    */
  def encode(serializer: ISerializer): Flow[JsValue, String, NotUsed] = Flow[JsValue] map { value =>
    serializer.encode(value)
  }

  /**
    * Returns a JsValue from a AWS Message.
    * @return The JsValue
    */
  def decode(serializer: ISerializer): Flow[Message, JsValue, NotUsed] = Flow[Message] map { message =>
    serializer.decode(message.getBody)
  }
}
