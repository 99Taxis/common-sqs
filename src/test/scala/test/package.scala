import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.testkit.{ImplicitSender, TestKit, TestKitBase}
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.taxis99.amazon.sqs.SqsClientFactory
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time._

import scala.concurrent.{ExecutionContext, Future}

package object test {
  trait BaseSpec extends FlatSpec with Matchers with OptionValues with PatienceConfiguration with RecoverMethods {
    implicit val defaultPatience =
      PatienceConfig(timeout =  Span(3, Seconds), interval = Span(5, Millis))
  }

  trait StreamSpec extends AsyncFlatSpec with Matchers with OptionValues with PatienceConfiguration
    with TestKitBase with ImplicitSender with BeforeAndAfterAll {

    implicit lazy val system = ActorSystem("test", ConfigFactory.parseString("""
        akka.actor.deployment.default.dispatcher = "akka.test.calling-thread-dispatcher"
      """))

    override implicit def executionContext: ExecutionContext = system.dispatcher
    
    override implicit def patienceConfig = PatienceConfig(timeout =  Span(1, Minute), interval = Span(5, Millis))

    val decider: Supervision.Decider = {
      case _ => Supervision.Stop
    }
    val settings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)

    implicit lazy val materializer = ActorMaterializer(settings)

    def withInMemoryQueue(testCode: (AmazonSQSAsync) => Future[Assertion]): Future[Assertion] = {
      val (_, aws) = SqsClientFactory.inMemory(system)
      testCode(aws) // "loan" the fixture to the test
    }

    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }
  }
}
