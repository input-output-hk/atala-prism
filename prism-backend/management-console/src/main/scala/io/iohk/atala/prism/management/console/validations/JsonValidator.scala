package io.iohk.atala.prism.management.console.validations

import io.circe.Json

import scala.concurrent.Future
import scala.util.Try

object JsonValidator {
  def jsonDataF(string: String): Future[Json] = {
    Future.fromTry {
      Try {
        val jsonData = Option(string).filter(_.nonEmpty).getOrElse("{}")
        io.circe.parser
          .parse(jsonData)
          .getOrElse(throw new RuntimeException("Invalid jsonData: it must be a JSON string"))
      }
    }
  }
}
