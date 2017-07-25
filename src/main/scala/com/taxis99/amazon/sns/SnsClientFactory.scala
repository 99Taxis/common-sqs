package com.taxis99.amazon.sns

import akka.actor.ActorSystem
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNSAsync, AmazonSNSAsyncClientBuilder}
import com.taxis99.amazon.ElasticMQ
import org.elasticmq.rest.sqs.SQSRestServer

object SnsClientFactory {

  /**
    * Returns a default Amazons SNS client.
    */
  def default(): AmazonSNSAsync = {
    AmazonSNSAsyncClientBuilder.defaultClient()
  }

  /**
    * Returns a Amazons SNS client connected to an endpoint at localhost.
    */
  def atLocalhost(port: Int = 9324): AmazonSNSAsync = {
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", "elasticmq")
    AmazonSNSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()
  }

  def inMemory(actorSystem: ActorSystem): (SQSRestServer, AmazonSNSAsync) = {
    val server = ElasticMQ.inMemory()
    val port = server.waitUntilStarted().localAddress.getPort()
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", s"elasticmq-$port")
    val conn = AmazonSNSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()

    (server, conn)
  }
}
