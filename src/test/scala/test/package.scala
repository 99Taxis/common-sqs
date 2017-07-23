import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit, TestKitBase}
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec, OptionValues}

package object test {
  trait BaseSpec extends FlatSpec with Matchers with OptionValues with PatienceConfiguration

  trait StreamSpec extends BaseSpec with TestKitBase with ImplicitSender with BeforeAndAfterAll {
    implicit lazy val system = ActorSystem("test")
    implicit lazy val materializer = ActorMaterializer()

    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }
  }
}
