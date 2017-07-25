package mocks

import akka.actor.ActorRef
import com.taxis99.sqs.{SqsClient, SqsConsumer}

import scala.concurrent.{ExecutionContext, Future}

class TestConsumer(queueName: String, probe: ActorRef)
                  (implicit val ec: ExecutionContext, val sqs: SqsClient) extends SqsConsumer[TestType] {
  def name = queueName

  def consume(message: TestType): Future[Unit] = Future {
    probe ! message
  }

  startConsumer()
}
