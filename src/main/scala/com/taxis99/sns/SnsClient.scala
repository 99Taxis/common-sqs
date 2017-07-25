package com.taxis99.sns

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sns.scaladsl.SnsPublisher
import akka.stream.scaladsl.Source
import com.amazonaws.services.sns.AmazonSNSAsync
import com.taxis99.streams.Producer
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@Singleton
class SnsClient @Inject()(config: Config)
                         (implicit actorSystem: ActorSystem, ec: ExecutionContext, sns: AmazonSNSAsync) {

  protected val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  implicit val materializer = ActorMaterializer()

  logger.info("SNS Client ready")

  def getTopic(topicKey: String) = Future {
    val topicName = config.getString(s"sqs.$topicKey")
    val topicArn = sns.createTopicAsync(topicName).get().getTopicArn
    SnsTopic(topicKey, topicName, topicArn)
  } andThen {
    case Failure(e) => logger.error("Could not fetch SQS url", e)
  }

  def producer(eventualtopicConfig: Future[SnsTopic]) = (value: JsValue) => eventualtopicConfig flatMap { t =>
    Source.single(value) via Producer() runWith SnsPublisher.sink(t.arn)
  }
}
