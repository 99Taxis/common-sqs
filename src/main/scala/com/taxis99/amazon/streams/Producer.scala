package com.taxis99.amazon.streams

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.taxis99.amazon.serializers.ISerializer
import play.api.libs.json.JsValue

object Producer {

  /**
    * Returns a producer flow graph.
    * @return A flow graph stage
    */
  def apply(serializer: ISerializer): Flow[JsValue, String, NotUsed] = Serializer.encode(serializer)
}
