package com.taxis99.amazon.streams

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.sqs.model.Message
import com.taxis99.amazon.serializers.ISerializer
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

object Serializer {

  class SerializerException(message: String, ex: Throwable) extends Exception(message, ex) {
    override def getMessage: String = s"$message: ${ex.getMessage}"
  }

  final class SerializerEncoderException(message: String, ex: Throwable) extends SerializerException(message, ex)
  final class SerializerDecoderException(message: String, ex: Throwable) extends SerializerException(message, ex)

  /**
    * Returns a string from a JsValue.
    * @return The byte array string
    */
  def encode(serializer: ISerializer): Flow[JsValue, String, NotUsed] = Flow[JsValue] map { value =>
    try {
      serializer.encode(value)
    } catch {
      case e: Exception =>
        throw new SerializerEncoderException(s"${serializer.getClass.getSimpleName} could not encode $value with", e)
    }
  }

  /**
    * Returns a JsValue from a AWS Message.
    * @return The JsValue
    */
  def decode(serializer: ISerializer): Flow[Message, JsValue, NotUsed] = Flow[Message] map { message =>
    try {
      serializer.decode(message.getBody)
    } catch {
      case e: Exception =>
        throw new SerializerEncoderException(s"${serializer.getClass.getSimpleName} could not decode ${message.getBody} with", e)
    }
  }
}
