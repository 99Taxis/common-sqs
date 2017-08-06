package it.mocks

import akka.actor.ActorRef
import com.taxis99.amazon.serializers.{ISerializer, MsgPack}
import com.taxis99.amazon.sqs.{SqsClient, SqsConsumer}

import scala.concurrent.{ExecutionContext, Future}

class TestConsumer(queueName: String, probe: ActorRef)
                  (implicit val ec: ExecutionContext, val sqs: SqsClient) extends SqsConsumer[TestType] {

  override def serializer: ISerializer = MsgPack

  def queue = queueName

  def consume(message: TestType): Future[Unit] = Future {
    probe ! message
  }

  startConsumer()
}
