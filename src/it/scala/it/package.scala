import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.testkit.{TestKit, TestKitBase}
import com.taxis99.amazon.sns.SnsClientFactory
import com.taxis99.amazon.sqs.SqsClientFactory
import com.typesafe.config.ConfigFactory
import org.scalatest._
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Minute, Span}

import scala.concurrent.ExecutionContext

package object it {

  trait IntegrationSpec extends AsyncFlatSpec with Matchers with OptionValues with PatienceConfiguration
    with TestKitBase with BeforeAndAfterAll {

    implicit lazy val system: ActorSystem = ActorSystem("test")

    override implicit def executionContext: ExecutionContext = system.dispatcher

    override implicit def patienceConfig = PatienceConfig(timeout =  Span(1, Minute), interval = Span(5, Millis))
    
    implicit lazy val amazonSqsConn = SqsClientFactory.atLocalhost(9324)
    implicit lazy val amazonSnsConn = SnsClientFactory.atLocalhost(9292)

    override def afterAll {
      TestKit.shutdownActorSystem(system)
    }
  }
}
