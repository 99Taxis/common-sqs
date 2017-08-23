import akka.Done
import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestKitBase}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import com.amazonaws.services.sqs.model.{PurgeQueueRequest, SendMessageRequest, SendMessageResult}
import com.taxis99.amazon.sns.SnsClientFactory
import com.taxis99.amazon.sqs.SqsClientFactory
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Minute, Span}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

package object it {

  trait IntegrationSpec extends AsyncFlatSpec with Matchers with OptionValues with PatienceConfiguration
    with TestKitBase with BeforeAndAfterEach with BeforeAndAfterAll {

    implicit lazy val system: ActorSystem = ActorSystem("test")

    override implicit def executionContext: ExecutionContext = system.dispatcher

    override implicit def patienceConfig = PatienceConfig(timeout =  Span(1, Minute), interval = Span(5, Millis))
    
    implicit lazy val amazonSqsConn = SqsClientFactory.atLocalhost(9324)
    implicit lazy val amazonSnsConn = SnsClientFactory.atLocalhost(9292)

    def produceRawMessage(queueName: String, body: String): Future[Done] = {
      val message = Promise[Done]
      Try {
        amazonSqsConn.getQueueUrl(queueName).getQueueUrl
      } map { url =>
        amazonSqsConn.sendMessageAsync(url, body,
          new AsyncHandler[SendMessageRequest, SendMessageResult] {
            override def onError(exception: Exception) = {
              message.failure(exception)
            }
            override def onSuccess(request: SendMessageRequest, result: SendMessageResult) = {
              message.success(Done)
            }
          })
      } recover {
        case e: Exception => message.failure(e)
      }
      message.future
    }

    def publishRawMessage(topicName: String, body: String): Future[Done] = {
      val message = Promise[Done]
      Try {
        amazonSnsConn.createTopic(topicName).getTopicArn
      } map { arn =>
        amazonSnsConn.publishAsync(arn, body, new AsyncHandler[PublishRequest, PublishResult] {
          override def onError(exception: Exception) = {
            message.failure(exception)
          }
          override def onSuccess(request: PublishRequest, result: PublishResult) = {
            message.success(Done)
          }
        })
      } recover {
        case e: Exception => message.failure(e)
      }
      message.future
    }

    override def beforeEach(): Unit = {
      // Purge queue messages before each test
      amazonSqsConn.listQueues().getQueueUrls.asScala.map { queueUrl =>
        amazonSqsConn.purgeQueue(new PurgeQueueRequest(queueUrl))
      }
      super.beforeEach()
    }

    override def afterAll {
      TestKit.shutdownActorSystem(system)
      super.afterAll()
    }
  }
}
