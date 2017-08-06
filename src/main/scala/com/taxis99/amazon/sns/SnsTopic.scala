package com.taxis99.amazon.sns

/**
  * A SNS topic configuration object
  * @param key  The topic configuration key
  * @param name The topic name at Amazon
  * @param arn  The topic ARN
  */
case class SnsTopic(key: String, name: String, arn: String)
