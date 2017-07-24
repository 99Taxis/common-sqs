package com.taxis99.sqs.streams

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sqs.model.Message
import msgpack4z._
import play.api.libs.json.{JsValue, Json}

object Serializer {

  val jsonVal = Play2Msgpack.jsValueCodec(PlayUnpackOptions.default)

  def pack(value: JsValue): String = {
    val packer = MsgOutBuffer.create()
    jsonVal.pack(packer, value)
    packer.result().map(_.toInt).mkString(" ")
  }

  def unpack(value: String): JsValue = {
    val bytes: Array[Byte] = value.split(" ").map(_.toByte)
    val unpacker = MsgInBuffer.apply(bytes)
    val unpacked = jsonVal.unpack(unpacker)
    unpacked.valueOr { e =>
      new ClassCastException("Could not decode message")
      Json.obj()
    }
  }

  /**
    * Compress a JSON it with message pack and return an string representation of the result
    * @return The byte array string
    */
  def encode: Flow[JsValue, String, NotUsed] = Flow[JsValue] map { value =>
    pack(value)
  }

  /**
    * 
    * @return
    */
  def decode: Flow[Message, JsValue, NotUsed] = Flow[Message] map { value =>
    unpack(value.getBody)
  }
}
