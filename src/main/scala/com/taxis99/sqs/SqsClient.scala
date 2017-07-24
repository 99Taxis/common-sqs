package com.taxis99.sqs

import javax.inject._

import net.ceedubs.ficus.Ficus._
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sqs.{All, MessageAttributeName, SqsSourceSettings}
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSink, SqsSource}
import akka.stream.scaladsl.Source
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.taxis99.sqs.streams.{Consumer, Producer, Serializer}
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class SqsClient @Inject()(config: Config)
                         (implicit actorSystem: ActorSystem, ec: ExecutionContext, sqs: AmazonSQSAsync) {

  protected val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName))

  implicit val materializer = ActorMaterializer()

  private val defaultWaitTimeSeconds = config.as[Option[Int]]("sqs.settings.default.waitTimeSeconds").getOrElse(20)
  private val defaultMaxBufferSize = config.as[Option[Int]]("sqs.settings.default.maxBufferSize").getOrElse(100)
  private val defaultMaxBatchSize = config.as[Option[Int]]("sqs.settings.default.maxBatchSize").getOrElse(10)
  private val defaultMaxRetries = config.as[Option[Int]]("sqs.settings.default.maxRetries").getOrElse(200)

  private def getQueue(queueConfigKey: String): Future[SqsQueue] = ???
  
  def consumer[A](eventualQueueConfig: Future[SqsQueue])
                 (block: JsValue => Future[A]) = eventualQueueConfig flatMap { q =>
    // Get configuration options
    val waitTimeSeconds = config.as[Option[Int]](s"sqs.settings.${q.key}.waitTimeSeconds").getOrElse(defaultWaitTimeSeconds)
    val maxBufferSize = config.as[Option[Int]](s"sqs.settings.${q.key}.maxBufferSize").getOrElse(defaultMaxBufferSize)
    val maxBatchSize = config.as[Option[Int]](s"sqs.settings.${q.key}.maxBatchSize").getOrElse(defaultMaxBatchSize)
    val maxRetries = config.as[Option[Int]](s"sqs.settings.${q.key}.maxRetries").getOrElse(defaultMaxRetries)

    // Configure source to send all attributes from the message
    val sqsSettings = new SqsSourceSettings(waitTimeSeconds, maxBufferSize, maxBatchSize,
      attributeNames = Seq(All), messageAttributeNames = Seq(MessageAttributeName("All")))

    SqsSource(q.url, sqsSettings) via Consumer(Duration.Zero)(block) runWith SqsAckSink(q.url)
  }

  def producer(eventualQueueConfig: Future[SqsQueue]) = (value: JsValue) => eventualQueueConfig flatMap { q =>
    Source.single(value) via Serializer.encode runWith SqsSink(q.url)
  }
}
