package com

import com.typesafe.config.Config

import scala.util.Try

package object taxis99 {
  object implicits {

    val DISABLE_BUFFER: Int = 0

    implicit class OptionalConfiguration(config: Config) {
      def getOptionalInt(path: String) = Try {
        config.getInt(path)
      }.toOption
    }
  }
}
