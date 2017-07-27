package com.taxis99.amazon.sqs

import javax.inject._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSink, SqsSource}
import akka.stream.alpakka.sqs.{All, MessageAttributeName, SqsSourceSettings}
import akka.stream.scaladsl.Source
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.{GetQueueUrlRequest, GetQueueUrlResult}
import com.taxis99.amazon.streams.{Consumer, Producer}
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import net.ceedubs.ficus.Ficus._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Failure

@Singleton
class SqsClient @Inject()(config: Config)
                         (implicit actorSystem: ActorSystem, ec: ExecutionContext, sqs: AmazonSQSAsync) {

  protected val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  implicit val materializer = ActorMaterializer()

  private val defaultWaitTimeSeconds = config.as[Option[Int]]("sqs.settings.default.waitTimeSeconds").getOrElse(20)
  private val defaultMaxBufferSize = config.as[Option[Int]]("sqs.settings.default.maxBufferSize").getOrElse(100)
  private val defaultMaxBatchSize = config.as[Option[Int]]("sqs.settings.default.maxBatchSize").getOrElse(10)
  private val defaultMaxRetries = config.as[Option[Int]]("sqs.settings.default.maxRetries").getOrElse(200)

  logger.info("SQS Client ready")

  def getQueue(queueKey: String): Future[SqsQueue] = {
    val promise = Promise[SqsQueue]
    val queueName = config.getString(s"sqs.$queueKey")
    sqs.getQueueUrlAsync(queueName, new AsyncHandler[GetQueueUrlRequest, GetQueueUrlResult] {
      override def onError(exception: Exception): Unit = promise.failure(exception)

      override def onSuccess(request: GetQueueUrlRequest, result: GetQueueUrlResult): Unit =
        promise.success(SqsQueue(queueKey, queueName, result.getQueueUrl))
    })
    promise.future
  }
  
  def consumer[A](eventualQueueConfig: Future[SqsQueue])
                 (block: JsValue => Future[A]) = eventualQueueConfig flatMap { q =>

    logger.info(s"Start consuming queue ${q.url}")
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
    Source.single(value) via Producer() runWith SqsSink(q.url)
  }
}
