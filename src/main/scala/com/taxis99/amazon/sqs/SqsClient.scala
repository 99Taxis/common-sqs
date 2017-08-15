package com.taxis99.amazon.sqs

import javax.inject._

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsSink, SqsSource}
import akka.stream.alpakka.sqs.{All, MessageAttributeName, SqsSourceSettings}
import akka.stream.scaladsl.Source
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.{GetQueueUrlRequest, GetQueueUrlResult}
import com.taxis99.amazon.serializers.ISerializer
import com.taxis99.amazon.streams.{Consumer, Producer}
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import net.ceedubs.ficus.Ficus._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * The SQS client provides an abstraction to interact with Amazon SQS queues through Akka Stream. The client is
  * responsible to manage the queue configuration automatically fetching its information from AWS directly.
  * @param config      The configuration object
  * @param actorSystem The actor system
  * @param ec          The default execution context
  * @param sqs         The Amazon SQS Async client
  */
@Singleton
class SqsClient @Inject()(config: Config)
                         (implicit actorSystem: ActorSystem, ec: ExecutionContext, sqs: AmazonSQSAsync) {

  protected val logger: Logger = Logger(LoggerFactory.getLogger(getClass.getName))

  protected val resumeOnFailure: Supervision.Decider = {
    case e: Exception => {
      logger.error("SQS flow raised an exception", e)
      Supervision.Resume
    }
  }
  protected val settings = ActorMaterializerSettings(actorSystem).withSupervisionStrategy(resumeOnFailure)

  implicit val materializer = ActorMaterializer(settings)

  private val defaultWaitTimeSeconds = config.as[Option[Int]]("sqs.settings.default.waitTimeSeconds").getOrElse(20)
  private val defaultMaxBufferSize = config.as[Option[Int]]("sqs.settings.default.maxBufferSize").getOrElse(100)
  private val defaultMaxBatchSize = config.as[Option[Int]]("sqs.settings.default.maxBatchSize").getOrElse(10)
  private val defaultMaxRetries = config.as[Option[Int]]("sqs.settings.default.maxRetries").getOrElse(200)

  logger.info("SQS Client ready")

  /**
    * Returns a SQS configuration object from the given configuration key. This method will fetch automatically
    * the queue name and URL from Amazon.
    * @param queueKey The queue configuration key
    * @return The eventual SQS configuration object
    */
  def getQueue(queueKey: String): Future[SqsQueue] = {
    val promise = Promise[SqsQueue]
    val queueName = config.getString(s"sqs.$queueKey")
    sqs.getQueueUrlAsync(queueName, new AsyncHandler[GetQueueUrlRequest, GetQueueUrlResult] {
      override def onError(exception: Exception): Unit = {
        logger.error(s"Could not fetch queue url for $queueName", exception.getCause)
        promise.failure(exception)
      }

      override def onSuccess(request: GetQueueUrlRequest, result: GetQueueUrlResult): Unit = {
        logger.debug(s"Retrieved queue url for $queueName: ${result.getQueueUrl}")
        promise.success(SqsQueue(queueKey, queueName, result.getQueueUrl))
      }
    })
    promise.future
  }

  /**
    * Materializes a consumer flow from a SQS source, handling the Ack of messages when all steps are successful.
    * @param eventualQueueConfig The future with the queue configuration object
    * @param block The consumer function to be applied to all incoming messages
    * @tparam A The expected consumer class type
    * @return The eventual consumer end event
    */
  def consumer[A](eventualQueueConfig: Future[SqsQueue], serializer: ISerializer)
                 (block: JsValue => Future[A]): Future[Done] = eventualQueueConfig flatMap { q =>
    logger.info(s"Start consuming queue ${q.url} with ${serializer.getClass.getSimpleName} serializer")
    // Get configuration options
    val waitTimeSeconds = config.as[Option[Int]](s"sqs.settings.${q.key}.waitTimeSeconds").getOrElse(defaultWaitTimeSeconds)
    val maxBufferSize = config.as[Option[Int]](s"sqs.settings.${q.key}.maxBufferSize").getOrElse(defaultMaxBufferSize)
    val maxBatchSize = config.as[Option[Int]](s"sqs.settings.${q.key}.maxBatchSize").getOrElse(defaultMaxBatchSize)
    val maxRetries = config.as[Option[Int]](s"sqs.settings.${q.key}.maxRetries").getOrElse(defaultMaxRetries)

    // Configure source to send all attributes from the message
    val sqsSettings = new SqsSourceSettings(waitTimeSeconds, maxBufferSize, maxBatchSize,
      attributeNames = Seq(All), messageAttributeNames = Seq(MessageAttributeName("All")))

    SqsSource(q.url, sqsSettings) via Consumer(serializer, Duration.Zero, maxRetries)(block) runWith SqsAckSink(q.url)
  }

  /**
    * Materializes a producer flow to a SQS sink.
    * @param eventualQueueConfig The future with the queue configuration object
    * @return A producer function for the given queue configuration
    */
  def producer(eventualQueueConfig: Future[SqsQueue], serializer: ISerializer): (JsValue) => Future[Done] = (value: JsValue) => {
    eventualQueueConfig flatMap { q =>
      logger.debug(s"Producing message $value with ${serializer.getClass.getSimpleName} serializer at ${q.name}")
      Source.single(value) via Producer(serializer) runWith SqsSink(q.url)
    }
  }
}
