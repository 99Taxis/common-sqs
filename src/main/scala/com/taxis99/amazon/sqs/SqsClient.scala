package com.taxis99.amazon.sqs

import javax.inject._

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.sqs.scaladsl.{SqsAckSink, SqsFlow, SqsSource}
import akka.stream.alpakka.sqs.{All, MessageAttributeName, SqsSourceSettings}
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy, Supervision}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.{GetQueueUrlRequest, GetQueueUrlResult}
import com.taxis99.amazon.serializers.ISerializer
import com.taxis99.amazon.streams.{Consumer, Producer}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsValue

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

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

  private val defaultWaitTimeSeconds = config.getInt("sqs.settings.default.waitTimeSeconds")
  private val defaultMaxBufferSize = config.getInt("sqs.settings.default.maxBufferSize")
  private val defaultMaxBatchSize = config.getInt("sqs.settings.default.maxBatchSize")
  private val defaultMaxRetries = config.getInt("sqs.settings.default.maxRetries")

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
  def consumer[A](eventualQueueConfig: Future[SqsQueue])(block: JsValue => Future[A])
                 (implicit serializer: ISerializer): Future[Done] = eventualQueueConfig flatMap { q =>
    logger.info(s"Start consuming queue ${q.url} with ${serializer.getClass.getSimpleName} serializer")
    // Get configuration options
    val waitTimeSeconds = Try(config.getInt(s"sqs.settings.${q.key}.waitTimeSeconds")).toOption.getOrElse(defaultWaitTimeSeconds)
    val maxBufferSize = Try(config.getInt(s"sqs.settings.${q.key}.maxBufferSize")).toOption.getOrElse(defaultMaxBufferSize)
    val maxBatchSize = Try(config.getInt(s"sqs.settings.${q.key}.maxBatchSize")).toOption.getOrElse(defaultMaxBatchSize)
    val maxRetries = Try(config.getInt(s"sqs.settings.${q.key}.maxRetries")).toOption.getOrElse(defaultMaxRetries)

    // Configure source to send all attributes from the message
    val sqsSettings = new SqsSourceSettings(waitTimeSeconds, maxBufferSize, maxBatchSize,
      attributeNames = Seq(All), messageAttributeNames = Seq(MessageAttributeName("All")))

    SqsSource(q.url, sqsSettings) via Consumer(Duration.Zero, maxRetries)(block) runWith SqsAckSink(q.url)
  }

  /**
    * Materializes a producer flow to a SQS sink.
    * @param eventualQueueConfig The future with the queue configuration object
    * @return A producer function for the given queue configuration
    */
  def producer(eventualQueueConfig: Future[SqsQueue])
              (implicit serializer: ISerializer): Future[SourceQueueWithComplete[(JsValue, Promise[Done])]] =
    eventualQueueConfig map { q =>
      val flow = SqsFlow(q.url)
      logger.info(s"Start producer for ${q.url}")
      Source.queue[(JsValue, Promise[Done])](0, OverflowStrategy.backpressure).async to Producer.sqs(flow) run()
    }
}
