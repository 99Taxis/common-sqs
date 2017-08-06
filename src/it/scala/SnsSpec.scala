import akka.testkit.TestProbe
import com.taxis99.amazon.sns.SnsClient
import com.taxis99.amazon.sqs.SqsClient
import com.typesafe.config.ConfigFactory
import it.IntegrationSpec
import it.mocks.{TestConsumer, TestPublisher, TestType}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
  * Topic definition and subscriptions at resources folder
  * @see ./src/it/resources/sns.json
  */
class SnsSpec extends IntegrationSpec {

  val config = ConfigFactory.parseMap(Map(
    "sns.topic" -> "integration-test-topic",
    "sqs.q1" -> "sns-test-q-1",
    "sqs.q2" -> "sns-test-q-2"
  ).asJava)

  implicit val sqs = new SqsClient(config)
  implicit val sns = new SnsClient(config)

  val probeA = TestProbe()
  val probeB = TestProbe()

  val publisher = new TestPublisher("topic")
  val consumer1 = new TestConsumer("q1", probeA.ref)
  val consumer2 = new TestConsumer("q2", probeB.ref)

  it should "publish a message to an topic and deliver to all queues subscribed" in {
    val msg = TestType("bar", 100)

    publisher.publish(msg) map { _ =>
      probeA expectMsg (10.seconds, msg)
      probeB expectMsg (10.seconds, msg)
      succeed
    }
  }
}
