package com.taxis99.amazon.sqs

/**
  * A SQS queue configuration object
  * @param key  The queue configuration key
  * @param name The queue name at Amazon
  * @param url  The queue URL
  */
case class SqsQueue(key: String, name: String, url: String)
