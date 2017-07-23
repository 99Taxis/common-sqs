package com.taxis99.sqs

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSink, SqsSource}
import akka.stream.scaladsl.Source
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.taxis99.sqs.streams.{Consumer, Producer}
import com.typesafe.config.Config
import play.api.libs.json.JsValue

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SqsClient @Inject()(config: Config)
                         (implicit actorSystem: ActorSystem, ec: ExecutionContext, sqs: AmazonSQSAsync) {

  implicit val materializer = ActorMaterializer()

  private def getQueue(queueConfigKey: String): Future[SqsQueue] = ???
  
  def consumer[A](eventualQueueConfig: Future[SqsQueue], retryDelay: Duration = Duration.Zero)
                 (block: JsValue => Future[A]) = {
    // TODO Configure the source properly
    SqsSource("") via Consumer(retryDelay)(block) runWith SqsAckSink("")
  }

  def producer(eventualQueueConfig: Future[SqsQueue]) = (value: JsValue) =>
    Source.single(value) via Producer() runWith SqsSink("")

}
