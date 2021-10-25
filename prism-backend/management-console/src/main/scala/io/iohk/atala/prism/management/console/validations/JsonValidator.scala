package io.iohk.atala.prism.management.console.validations

import io.circe.{Decoder, Json}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object JsonValidator {
  def parse(string: String): Try[Json] =
    Try {
      io.circe.parser
        .parse(string)
        .getOrElse(
          throw new RuntimeException("Invalid json: it must be a JSON string")
        )
    }

  def jsonData(string: String): Try[Json] = {
    val jsonData = Option(string).filter(_.nonEmpty).getOrElse("{}")
    parse(jsonData)
  }

  def jsonDataF(string: String): Future[Json] = {
    Future.fromTry(jsonData(string))
  }

  def extractField[A: Decoder](json: Json)(name: String): Try[A] = {
    json.hcursor
      .downField(name)
      .as[A]
      .fold(
        _ => Failure(new RuntimeException(s"Failed to parse $name")),
        Success.apply
      )
  }

  def extractFieldWith[A: Decoder, B](
      json: Json
  )(name: String)(f: A => B): Try[B] = {
    extractField(json)(name).map(f)
  }

  def extractFieldWithTry[A: Decoder, B](
      json: Json
  )(name: String)(f: A => Try[B]): Try[B] = {
    extractField(json)(name).flatMap(f)
  }
}
