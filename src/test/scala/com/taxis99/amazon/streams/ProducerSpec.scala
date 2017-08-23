package com.taxis99.amazon.streams

import java.util.UUID

import akka.stream.alpakka.sqs.scaladsl.Result
import akka.stream.scaladsl.{Flow, Source}
import akka.testkit.TestProbe
import akka.{Done, NotUsed}
import com.amazonaws.services.sns.model.PublishResult
import com.amazonaws.services.sqs.model.SendMessageResult
import com.taxis99.amazon.serializers.{ISerializer, PlayJson}
import play.api.libs.json.Json
import test.StreamSpec

import scala.concurrent.Promise

class ProducerSpec extends StreamSpec {

  implicit val serializer: ISerializer = PlayJson
  
  val msg = Json.obj("foo" -> "bar")

  ".sns" should "return a sink stage that push the message to the SQS flow and fulfil a promise according to the result" in {
    val probe = TestProbe()

    val sqsMockFlow: Flow[String, Result, NotUsed] = Flow[String] map { msg =>
      probe.ref ! msg

      val sendResult = new SendMessageResult()
        .withMessageId(UUID.randomUUID().toString)

      Result(sendResult, message = msg)
    }

    val done = Promise[Done]
    Source.single(msg -> done) to Producer.sqs(sqsMockFlow) run()
    
    done.future.map { _ =>
      probe expectMsg msg.toString()
      succeed
    }
  }

  ".sqs" should "return a sink stage that push the message to the SNS flow and fulfil a promise according to the result" in {
    val probe = TestProbe()
    
    val snsMockFlow: Flow[String, PublishResult, NotUsed] = Flow[String] map { msg =>
      probe.ref ! msg
      new PublishResult().withMessageId(UUID.randomUUID().toString)
    }

    val done = Promise[Done]
    Source.single(msg -> done) to Producer.sns(snsMockFlow) run()

    done.future.map { _ =>
      probe expectMsg msg.toString()
      succeed
    }

  }
}
