package com.taxis99.amazon.sns

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sns.scaladsl.{SnsPublisher => SnsSink}
import akka.stream.scaladsl.Source
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.model.{CreateTopicRequest, CreateTopicResult}
import com.taxis99.amazon.serializers.ISerializer
import com.taxis99.amazon.streams.Producer
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * The SNS client provides an abstraction to interact with Amazon SNS topics through Akka Stream. The client is
  * responsible to manage the topic configuration automatically fetching its information from AWS directly.
  * @param config      The configuration object
  * @param actorSystem The actor system
  * @param ec          The default execution context
  * @param sns         The Amazon SNS Async client
  */
@Singleton
class SnsClient @Inject()(config: Config)
                         (implicit actorSystem: ActorSystem, ec: ExecutionContext, sns: AmazonSNSAsync) {

  protected val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  implicit val materializer = ActorMaterializer()

  logger.info("SNS Client ready")

  /**
    * Returns a SNS configuration object from the given configuration key. This method will fetch automatically
    * the topic name and ARN from Amazon.
    * @param topicKey The topic configuration key
    * @return The eventual SNS configuration object
    */
  def getTopic(topicKey: String): Future[SnsTopic] = {
    val promise = Promise[SnsTopic]
    val topicName = config.getString(s"sns.$topicKey")
    sns.createTopicAsync(topicName, new AsyncHandler[CreateTopicRequest, CreateTopicResult] {
      override def onError(exception: Exception) =
        promise.failure(exception)
      override def onSuccess(request: CreateTopicRequest, result: CreateTopicResult) =
        promise.success(SnsTopic(topicKey, topicName, result.getTopicArn))
    })
    promise.future
  }

  /**
    * Materializes a publish flow to a SNS sink.
    * @param eventualTopicConfig The future with the topic configuration object
    * @return A publish function for the given queue configuration
    */
  def publisher(eventualTopicConfig: Future[SnsTopic], serializer: ISerializer) = (value: JsValue) => eventualTopicConfig flatMap { t =>
    Source.single(value) via Producer(serializer) runWith SnsSink.sink(t.arn)
  }
}
