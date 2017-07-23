package com.taxis99.sqs

import java.net.ServerSocket

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import org.elasticmq.rest.sqs.{SQSRestServer, SQSRestServerBuilder}

object SqsConnection {

  def build(): AmazonSQSAsync = {
    AmazonSQSAsyncClientBuilder.defaultClient()
  }

  def atLocalhost(port: Int = 9324): AmazonSQSAsync = {
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", "elasticmq")
    AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()
  }

  def inMemory(port: Int = 0): (SQSRestServer, AmazonSQSAsync) = {
    val p = if (port == 0) getPort else port
    val server = SQSRestServerBuilder.withPort(p).withInterface("localhost").start()
    val socket = server.waitUntilStarted().localAddress
    val endpoint = new EndpointConfiguration(s"http://${socket.getHostString}:${socket.getPort}", "elasticmq")
    val conn = AmazonSQSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()

    (server, conn)
  }

  private def getPort = {
    val s = new ServerSocket(0)
    val p = s.getLocalPort
    s.close()
    p
  }
}
