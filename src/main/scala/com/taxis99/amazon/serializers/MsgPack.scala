package com.taxis99.amazon.serializers

import java.util.Base64

import com.typesafe.scalalogging.Logger
import msgpack4z.{MsgOutBuffer, _}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}

object MsgPack extends ISerializer {
  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))

  protected val jsonVal = Play2Msgpack.jsValueCodec(PlayUnpackOptions.default)
  protected val b64encoder = Base64.getEncoder
  protected val b64decoder = Base64.getDecoder

  /**
    * Returns a MsgPack compressed string from a JsValue
    * @param value the JSON value
    * @return a byte array string representation of the JsValue
    */
  override def encode(value: JsValue): String = {
    val packer = MsgOutBuffer.create()
    jsonVal.pack(packer, value)
    b64encoder.encodeToString(packer.result())
  }

  /**
    * Returns a JsValue from a compressed MsgPack string.
    * @param value The byte array string
    * @return
    */
  override def decode(value: String): JsValue = {
    val bytes: Array[Byte] = b64decoder.decode(value)
    val unpacker = MsgInBuffer.apply(bytes)
    val unpacked = jsonVal.unpack(unpacker)
    unpacked.valueOr { e =>
      logger.error("Could not decode message", e)
      new ClassCastException("Could not decode message")
      Json.obj()
    }
  }
}
