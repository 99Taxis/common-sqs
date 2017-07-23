package com.taxis99.sqs

import javax.inject._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.alpakka.sqs.MessageAction
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSource}
import akka.stream.scaladsl.{Broadcast, GraphDSL, RunnableGraph, Zip}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.taxis99.sqs.streams.{Serializer, SqsMessage}
import com.typesafe.config.Config
import play.api.libs.json.JsValue

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SqsClient @Inject()(config: Config)
                         (implicit actorSystem: ActorSystem, ec: ExecutionContext, sqs: AmazonSQSAsync) {

  private def getQueue(queueConfigKey: String): Future[SqsQueue] = ???

  def consumer[A](eventualQueueConfig: Future[SqsQueue], retryDelay: Duration = Duration.Zero)
                 (block: JsValue => Future[A]) = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val in = SqsSource("")
      val out = SqsAckSink("")

      val filter = builder.add(SqsMessage.retryFilter(200))
      val bcast = builder.add(Broadcast[Message](2))
      val merge = builder.add(Zip[Message, MessageAction])

      in ~> filter

      // Max retries not reached
      filter.out(0) ~> bcast
      bcast.out(0) ~>                                                      merge.in0
      bcast.out(1) ~> Serializer.decode ~> SqsMessage.ackOrRetry(block) ~> merge.in1
      merge.out ~> out
      // Max retries exceeded
      filter.out(1) ~> SqsMessage.ack ~> out

      ClosedShape
    })
  }

  def producer(eventualQueueConfig: Future[SqsQueue]) = ???

}
