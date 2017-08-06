package com.taxis99.amazon.sns

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.sns.{AmazonSNSAsync, AmazonSNSAsyncClientBuilder}

object SnsClientFactory {

  /**
    * Returns a Amazons SNS client connected to an endpoint at localhost.
    */
  def atLocalhost(port: Int = 9292): AmazonSNSAsync = {
    val endpoint = new EndpointConfiguration(s"http://localhost:$port", "sns-elasticmq")
    AmazonSNSAsyncClientBuilder.standard()
      .withEndpointConfiguration(endpoint)
      .build()
  }
}
