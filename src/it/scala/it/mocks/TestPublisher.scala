package it.mocks

import com.taxis99.amazon.serializers.{ISerializer, MsgPack}
import com.taxis99.amazon.sns.{SnsClient, SnsPublisher}

import scala.concurrent.ExecutionContext

class TestPublisher(topicName: String)
                   (implicit val ec: ExecutionContext, val sns: SnsClient) extends SnsPublisher[TestType] {

  override def serializer: ISerializer = MsgPack

  def topic = topicName
}
