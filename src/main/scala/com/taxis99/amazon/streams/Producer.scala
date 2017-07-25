package com.taxis99.amazon.streams

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import play.api.libs.json.JsValue

object Producer {

  /**
    * Returns a producer flow graph.
    * @return A flow graph stage
    */
  def apply(): Flow[JsValue, String, NotUsed] = Serializer.encode
}
