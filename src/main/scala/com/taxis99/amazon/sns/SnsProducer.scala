package com.taxis99.amazon.sns

/**
  * Please use the SnsPublisher instead of the producer.
  */
@Deprecated
trait SnsProducer[T] extends SnsPublisher[T]
