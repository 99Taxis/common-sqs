package it.mocks

import play.api.libs.json.Json

case class TestType(foo: String, bar: Int)

object TestType {
  implicit val testTypeFormat = Json.format[TestType]
}
