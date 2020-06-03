package io.iohk.atala.cvp.webextension.activetab.models

import java.util.UUID

import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.parse

import scala.util.Try

/**
  * As the responses go through a different channel than the requests, we can tag them
  * with a unique id to match the response.
  *
  * @param tag a unique tag
  * @param model the actual model
  */
case class TaggedModel[T: Decoder](tag: UUID, model: T)

object TaggedModel {
  def decode[T: Decoder](string: String): Try[TaggedModel[T]] = {
    parse(string).toTry
      .flatMap(_.as[TaggedModel[T]].toTry)
  }
}
