import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.testkit.{TestKit, TestKitBase}
import com.taxis99.amazon.sqs.SqsClientFactory
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Minute, Span}

import scala.concurrent.ExecutionContext

package object it {

  trait IntegrationSpec extends AsyncFlatSpec with Matchers with OptionValues with PatienceConfiguration
    with TestKitBase with BeforeAndAfterAll {

    implicit lazy val system: ActorSystem = ActorSystem("test", ConfigFactory.parseString("""
        akka.actor.deployment.default.dispatcher = "akka.test.calling-thread-dispatcher"
      """))

    override implicit def executionContext: ExecutionContext = system.dispatcher

    override implicit def patienceConfig = PatienceConfig(timeout =  Span(1, Minute), interval = Span(5, Millis))
    
    implicit lazy val aws = SqsClientFactory.atLocalhost()

    val decider: Supervision.Decider = {
      case _ => Supervision.Stop
    }
    val settings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)

    implicit lazy val materializer = ActorMaterializer(settings)

    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }
  }
}
