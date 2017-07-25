package com.taxis99.amazon.sqs

import akka.actor.ActorSystem
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import com.taxis99.amazon.ElasticMQ
import org.elasticmq.rest.sqs.SQSRestServer

object SqsClientFactory {

  /**
    * Returns a default Amazons SQS client.
    */
  def default(): AmazonSQSAsync = {
    AmazonSQSAsyncClientBuilder.defaultClient()
  }

  /**
    * Returns a Amazons SQS client connected to an endpoint at localhost.
    */
  def atLocalhost(port: Int = 9324): AmazonSQSAsync = {
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", "elasticmq")
    AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()
  }

  def inMemory(actorSystem: ActorSystem): (SQSRestServer, AmazonSQSAsync) = {
    val server = ElasticMQ.inMemory()
    val port = server.waitUntilStarted().localAddress.getPort()
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", s"elasticmq-$port")
    val conn = AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()

    (server, conn)
  }
}
