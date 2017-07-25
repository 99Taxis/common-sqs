import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.testkit.{ImplicitSender, TestKit, TestKitBase}
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.taxis99.amazon.ElasticMQ
import com.taxis99.amazon.sns.SnsClientFactory
import com.taxis99.amazon.sqs.SqsClientFactory
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time._

package object test {
  trait BaseSpec extends FlatSpec with Matchers with OptionValues with PatienceConfiguration with RecoverMethods {
    implicit val defaultPatience =
      PatienceConfig(timeout =  Span(3, Seconds), interval = Span(5, Millis))
  }

  trait StreamSpec extends BaseSpec with TestKitBase with ImplicitSender with BeforeAndAfterAll {
    implicit lazy val system = ActorSystem("test")

    val decider: Supervision.Decider = {
      case _ => Supervision.Stop
    }
    val settings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)

    implicit lazy val materializer = ActorMaterializer(settings)

    def withInMemoryQueue(testCode: (AmazonSQSAsync) => Any): Unit = {
      val server = ElasticMQ.inMemory()
      val port = server.waitUntilStarted().localAddress.getPort
      val aws = SqsClientFactory.atLocalhost(port)
      testCode(aws) // "loan" the fixture to the test
    }

    def withInMemoryTopic(testCode: (AmazonSNSAsync) => Any): Unit = {
      val server = ElasticMQ.inMemory()
      val port = server.waitUntilStarted().localAddress.getPort
      val aws = SnsClientFactory.atLocalhost(port)
      testCode(aws) // "loan" the fixture to the test
    }

    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }
  }
}
