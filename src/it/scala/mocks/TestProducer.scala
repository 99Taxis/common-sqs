package mocks

import com.taxis99.sqs.{SqsClient, SqsProducer}

import scala.concurrent.ExecutionContext

class TestProducer(queueName: String)
                  (implicit val ec: ExecutionContext, val sqs: SqsClient) extends SqsProducer[TestType] {

  def name = queueName
}
