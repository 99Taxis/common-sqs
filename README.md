Common SQS
===

A common library to abstract the Amazon SQS and SNS producers/consumers interactions.

**Features:**

- Auto SQS url discovery;
- Auto SNS topic ARN discovery;
- Back-pressure out of the box (via Akka Streams);
- Optional message compression (via MsgPack);

[![License](http://img.shields.io/:license-Apache%202-red.svg)](https://github.com/99Taxis/common-sqs/blob/master/LICENSE "Apache 2.0 Licence") [![Bintray](https://img.shields.io/bintray/v/99taxis/maven/common-sqs.svg)](https://bintray.com/99taxis/maven/common-sqs/_latestVersion) [![Maintenance](https://img.shields.io/maintenance/yes/2017.svg)](https://github.com/99Taxis/common-sqs/commits/master) [![Build Status](https://travis-ci.org/99Taxis/common-sqs.svg?branch=master)](https://travis-ci.org/99Taxis/common-sqs)

## Usage

Add the package to your dependencies and the bintray resolver.

```sbtshell
libraryDependencies += "com.taxis99" %% "common-sqs" % "0.3.3"
resolvers += Resolver.bintrayRepo("99taxis", "maven")
```

Configure the queues in your configuration file (assuming you are using TypeSafe Config).

```hocon
sqs {
    my-queue = "my-queue-name" 
    another-queue   = "queues-are-great"
}

sns {
    my-topic = "my-cool-topic"
}
```

At last but not least you have to implement the `SqsConsumer[T]` and `SqsProducer[T]` traits. Both the consumers and producers are strong typed, the client handles the serialization under the hood, but you must define a type that can be serialized and deserialized in `JsValue`:

```scala
package models

import play.api.libs.json.Json

case class MyCustomType(foo: String, bar: Int)

object MyCustomType {
  implicit val myCustomTypeFormat = Json.format[MyCustomType]
}
```

```scala
package consumers

import javax.inject.{Inject, Singleton}

import com.taxis99.amazon.sqs.{SqsClient, SqsConsumer}
import models.MyCustomType

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyConsumer @Inject()(implicit val ec: ExecutionContext, val sqs: SqsClient) 
  extends SqsConsumer[MyCustomType] {
  
  def queue = "my-queue"

  def consume(message: MyCustomType) = ???

  // Starts the consumer when the class is initialized
  startConsumer()
}
```

```scala
package producers

import javax.inject.{Inject, Singleton}

import com.taxis99.amazon.sqs.{SqsClient, SqsProducer}
import models.MyCustomType

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyProducer @Inject()(implicit val ec: ExecutionContext, val sqs: SqsClient) 
  extends SqsProducer[MyCustomType] {
  
  def queue = "my-queue"
}
```

```scala
package notifications

import javax.inject.{Inject, Singleton}

import com.taxis99.amazon.sns.{SnsClient, SnsPublisher}
import models.MyCustomType

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyNotification @Inject()(implicit val ec: ExecutionContext, val sns: SnsClient) 
  extends SnsPublisher[MyCustomType] {
  
  def topic = "my-topic"
}
```

#### Play Framework

Since this API relies at the JSR-330 Dependency Injection interface, the integration with the Play Framework using Guice is quite straightforward.

One just need to create an `AmazonSQSClientAsync` and register your instances at the application `Module`. 

```scala
import com.google.inject.{AbstractModule, Provides}
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClientBuilder}
import com.amazonaws.services.sqs.{AmazonSNSAsync, AmazonSNSAsyncClientBuilder}
import com.typesafe.config.Config
import com.taxis99.amazon.sqs.SqsClientFactory
import play.api.{Configuration, Environment}
import play.api.Mode.Prod
import consumers.MyConsumer
import producers.MyProducer

class Module extends AbstractModule {

  
  @Provides
  def amazonSqsClient(env: Environment): AmazonSQSAsync = {
    if (env.mode == Prod) {
      AmazonSQSAsyncClientBuilder.defaultClient()
    } else {
      SqsClientFactory.atLocalhost()
    }
  }
  
  @Provides
    def amazonSnsClient(env: Environment): AmazonSNSAsync = {
      if (env.mode == Prod) {
        AmazonSNSAsyncClientBuilder.defaultClient()
      } else {
        SqsClientFactory.atLocalhost()
      }
    }
  
  @Provides
  def config(config: Configuration): Config = config.underlying
  
  def configure = {
    bind(classOf[MyConsumer]).asEagerSingleton()
    bind(classOf[MyProducer]).asEagerSingleton()
  }
}
```

### Message compression

If you wish to use the advanced message compression (only for version `0.2.x`), you can specify the `MsgPack` serialization at your consumers, producers and publishers:

```scala
package producers

import javax.inject.{Inject, Singleton}

import com.taxis99.amazon.serializers.{ISerializer, MsgPack}
import com.taxis99.amazon.sqs.{SqsClient, SqsProducer}
import models.MyCustomType

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyProducer @Inject()(implicit val ec: ExecutionContext, val sqs: SqsClient) 
  extends SqsProducer[MyCustomType] {

  override def serializer: ISerializer = MsgPack
   
  def queue = "my-queue"
}
```

Take in consideration that your consumers and producers **MUST** specify the same serialization method to work properly.

## Development & Build

The best way to develop is through a TDD style, the test uses a in memory ElasticMQ to run the flows, allowing a fast interaction with the code.

Other caveat is to run the tests against several scala versions: 

```shell
$ sbt
> // Run tests against Scala 2.11 and 2.12
> + test
```

**Integration tests**

First you must launch an instance of the ElasticMQ server at `localhost:9324` then run the tests.

```shell
$ docker-compose up -d
$ sbt it:test
```

**Building**

To cross compile the `jar` for Scala 2.11 and 2.12 use the `+` modifier.

```shell
$ sbt "+ package"
```

## License

`common-sqs` is open source software released under the Apache 2.0 License by **99Taxis**.

See the [LICENSE](https://github.com/99Taxis/common-sqs/blob/master/LICENSE) file for details.

