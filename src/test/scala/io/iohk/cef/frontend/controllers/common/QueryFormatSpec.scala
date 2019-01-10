package io.iohk.cef.frontend.controllers.common

import io.iohk.cef.data.query.DataItemQuery._
import io.iohk.cef.data.query._
import org.scalatest.MustMatchers._
import org.scalatest.WordSpec
import play.api.libs.json.Json

class QueryFormatSpec extends WordSpec {

  import Codecs._

  "valueQueryFormat" should {
    "serialize and deserialize" in {
      val values: List[Value] = List(1, 1L, 1.toShort, 1.toByte, 2.0f, 2.0d, 'a', "string", true)

      values.foreach { input =>
        val output = Json.toJson(input)
        output.as[Value] must be(input)
      }
    }
  }

  "predicateQueryFormat" should {
    "serialize and deserialize" in {
      val input: Predicate = (Field(1) #== 1)
        .or(Field(2) #== 3L)
        .and(
          (Field(2) #== "iohk")
            .and(Field(3) #== 9.0d)
        )

      val output = Json.toJson(input)(createPredicateQueryFormat)
      output.as[Predicate](createPredicateQueryFormat) must be(input)
    }
  }

  "QueryFormat" should {
    "serialize and deserialize" in {
      val input: DataItemQuery = (Field(9) #== true)
        .or(Field(1) #== 1)
        .and(Field(2) #== 2.0d)
        .or(Field(1) #== 2.0f)
        .or {
          (Field(2) #== 3L)
            .or(Field(2) #== 2.toShort)
            .or(Field(2) #== 2.toByte)
            .or(Field(2) #== 'a')
            .or(Field(2) #== "string")
        }

      val serialized = Json.toJson(input)
      val deserialized = serialized.as[DataItemQuery]
      deserialized mustEqual input
    }
  }
}
