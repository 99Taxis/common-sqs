package com.taxis99.sqs.streams

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import play.api.libs.json.JsValue

object Producer {

  /**
    * Returns a producer flow graph.
    * @return A flow graph stage
    */
  def apply(): Flow[JsValue, String, NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val serializer = b.add(Serializer.encode)

    FlowShape(serializer.in, serializer.out)
  })
}
