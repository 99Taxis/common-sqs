import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.testkit.{ImplicitSender, TestKit, TestKitBase}
import org.scalatest.concurrent.Futures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, OptionValues}

package object test {
  trait BaseSpec extends FlatSpec with Matchers with OptionValues with Futures

  trait StreamSpec extends BaseSpec with TestKitBase with ImplicitSender with BeforeAndAfterAll {
    implicit lazy val system = ActorSystem("test")

    val decider: Supervision.Decider = {
      case _ => Supervision.Resume
    }
    val settings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)

    implicit lazy val materializer = ActorMaterializer(settings)

    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }
  }
}
