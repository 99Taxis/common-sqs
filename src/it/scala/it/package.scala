import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.testkit.{TestKit, TestKitBase}
import com.taxis99.amazon.sqs.SqsClientFactory
import org.scalatest._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.ExecutionContext

package object it {

  trait BaseSpec extends AsyncFlatSpec with Matchers with OptionValues with PatienceConfiguration with RecoverMethods
    with ScalaFutures with TestKitBase with BeforeAndAfterAll {

    implicit override def patienceConfig = PatienceConfig(scaled(Span(2, Seconds)), scaled(Span(15, Millis)))
    implicit override def executionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(6))

    implicit lazy val system = ActorSystem("test")
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
