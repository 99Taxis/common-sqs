package com.taxis99.amazon.sns

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.Source
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.model.{CreateTopicRequest, CreateTopicResult}
import com.taxis99.amazon.streams.Producer
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Failure

@Singleton
class SnsClient @Inject()(config: Config)
                         (implicit actorSystem: ActorSystem, ec: ExecutionContext, sns: AmazonSNSAsync) {

  protected val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  implicit val materializer = ActorMaterializer()

  logger.info("SNS Client ready")

  def getTopic(topicKey: String): Future[SnsTopic] = {
    val promise = Promise[SnsTopic]
    val topicName = config.getString(s"sqs.$topicKey")
    sns.createTopicAsync(topicName, new AsyncHandler[CreateTopicRequest, CreateTopicResult] {
      override def onError(exception: Exception) =
        promise.failure(exception)
      override def onSuccess(request: CreateTopicRequest, result: CreateTopicResult) =
        promise.success(SnsTopic(topicKey, topicName, result.getTopicArn))
    } )
    promise.future
  }

  def producer(eventualTopicConfig: Future[SnsTopic]) = (value: JsValue) => eventualTopicConfig flatMap { t =>
    Source.single(value) via Producer() runWith SnsPublisher.sink(t.arn)
  }
}
