package it.mocks

import com.taxis99.amazon.sns.{SnsClient, SnsProducer}

import scala.concurrent.ExecutionContext

class TestPublisher(topicName: String)
                   (implicit val ec: ExecutionContext, val sns: SnsClient) extends SnsProducer[TestType] {
  def topic = topicName
}
