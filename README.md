Common SQS
===

A common library to abstract the Amazon SQS and SNS producers/consumers interactions.

## Usage

Add the package to your dependecies.

```sbtshell
libraryDependencies += "com.taxis99" %% "common-sqs" % "1.0.0"
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

import com.taxis99.sqs.{SqsClient, SqsConsumer}
import models.MyCustomType

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyConsumer @Inject()(implicit val ec: ExecutionContext, val sqs: SqsClient) extends SqsConsumer[MyCustomType] {
  def name = "key-of-my-queue"

  def consume(message: MyCustomType) = ???

  // Manually start the consumer when the class is initialized
  startConsumer()
}
```

```scala
package producers

import javax.inject.{Inject, Singleton}

import com.taxis99.sqs.{SqsClient, SqsProducer}
import models.MyCustomType

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MyProducer @Inject()(implicit val ec: ExecutionContext, val sqs: SqsClient) extends SqsProducer[MyCustomType] {
  def name = "key-of-my-queue"
}
```
