package com.taxis99.amazon.streams

import com.amazonaws.services.sqs.model.Message
import com.taxis99.amazon.serializers.ISerializer
import play.api.libs.json.JsValue

import scala.util.Try

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
  def encode(message: JsValue)(implicit serializer: ISerializer): Try[String] = Try(serializer.encode(message))
    .recover {
      case e: Exception =>
        throw new SerializerEncoderException(s"${serializer.getClass.getSimpleName} could not encode $message with", e)
    }

  /**
    * Returns a JsValue from a AWS Message.
    * @return The JsValue
    */
  def decode(message: Message)(implicit serializer: ISerializer): Try[JsValue] = Try(serializer.decode(message.getBody))
    .recover {
       case e: Exception =>
        throw new SerializerEncoderException(s"${serializer.getClass.getSimpleName} could not decode ${message.getBody} with", e)
    }
}
