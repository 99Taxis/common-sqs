package com.taxis99.sqs

trait SqsProducer[T] {
  def produce(message: T)
}
