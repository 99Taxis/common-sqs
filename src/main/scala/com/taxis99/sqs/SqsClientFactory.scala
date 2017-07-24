package com.taxis99.sqs

import akka.actor.ActorSystem
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import org.elasticmq.rest.sqs.{SQSLimits, SQSRestServer, SQSRestServerBuilder}

object SqsClientFactory {

  def default(): AmazonSQSAsync = {
    AmazonSQSAsyncClientBuilder.defaultClient()
  }

  def atLocalhost(port: Int = 9324): AmazonSQSAsync = {
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", "elasticmq")
    AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()
  }

  def inMemory(actorSystem: ActorSystem): (SQSRestServer, AmazonSQSAsync) = {
    val server = SQSRestServerBuilder.withDynamicPort()
        .withSQSLimits(SQSLimits.Relaxed).start()
    val port = server.waitUntilStarted().localAddress.getPort()
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", s"elasticmq-$port")
    val conn = AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()

    (server, conn)
  }
}
