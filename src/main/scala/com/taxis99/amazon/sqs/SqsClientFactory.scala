package com.taxis99.amazon.sqs

import akka.actor.ActorSystem
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import org.elasticmq.rest.sqs.{SQSLimits, SQSRestServer, SQSRestServerBuilder}

object SqsClientFactory {

  /**
    * Returns a Amazons SQS client connected to an endpoint at localhost.
    */
  def atLocalhost(port: Int = 9324): AmazonSQSAsync = {
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", "elasticmq")
    AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()
  }

  /**
    * Starts an in memory ElasticMQ server with a REST interface compatible with Amazon SQS.
    * @param actorSystem
    * @return
    */
  def inMemory(actorSystem: Option[ActorSystem] = None): (SQSRestServer, AmazonSQSAsync) = {
    val serverBuilder = SQSRestServerBuilder.withDynamicPort().withSQSLimits(SQSLimits.Relaxed)
    actorSystem foreach serverBuilder.withActorSystem
    val server = serverBuilder.start()
    val port = server.waitUntilStarted().localAddress.getPort()
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", s"elasticmq-$port")
    val conn = AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()

    (server, conn)
  }
}
