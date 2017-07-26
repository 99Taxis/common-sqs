Common SQS
===

A common library to abstract the Amazon SQS and SNS producers/consumers interactions.

**Features:**

- Auto SQS url discovery;
- Auto SNS topic ARN discovery;
- Back-pressure out of the box (via Akka Streams);
- Message compaction (via MsgPack);

[![License](http://img.shields.io/:license-Apache%202-red.svg)](https://github.com/99Taxis/common-sqs/blob/master/LICENSE "Apache 2.0 Licence") [![Bintray](https://img.shields.io/bintray/v/99taxis/maven/common-sqs.svg)](https://bintray.com/99taxis/maven/common-sqs/_latestVersion) [![Maintenance](https://img.shields.io/maintenance/yes/2017.svg)](https://github.com/99Taxis/common-sqs/commits/master)

## Usage

Add the package to your dependencies and the bintray resolver.

```sbtshell
libraryDependencies += "com.taxis99" %% "common-sqs" % "0.1.0"
resolvers += "bintray.99taxis OS releases" at "http://dl.bintray.com/content/99taxis/maven"
```

Configure the queues in your configuration file (assuming you are using TypeSafe Config).

```hocon
sqs {
    key-of-my-queue = "my-queue-name" 
    another-queue   = "queues-are-great"
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
class MyConsumer @Inject()(implicit val ec: ExecutionContext, val sqs: SqsClient) extends SqsConsumer[MyCustomType] {
  def queue = "key-of-my-queue"

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
class MyProducer @Inject()(implicit val ec: ExecutionContext, val sqs: SqsClient) extends SqsProducer[MyCustomType] {
  def name = "key-of-my-queue"
}
```

#### Play Framework

Since this API relies at the JSR-330 Dependency Injection interface, the integration with the Play Framework using Guice is quite straightforward.

One just need to create an `AmazonSQSClientAsync` or use `SqsClientFactory` to do so, and register your instances at the application `Module`. 

```scala
import com.google.inject.{AbstractModule, Provides}
import com.amazonaws.services.sqs.AmazonSQSAsync
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
      SqsClientFactory.default()
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

`common-sqs` is open source software released under the Apache 2.0 License.

See the [LICENSE](https://github.com/99Taxis/common-sqs/blob/master/LICENSE) file for details.

