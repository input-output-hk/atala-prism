package io.iohk.atala.mirror.models

import io.circe._
import io.circe.generic.semiauto._

case class CardanoBlockId(id: Long) extends AnyVal

object CardanoBlockId {
  implicit val cardanoBlockIdEncoder: Encoder[CardanoBlockId] = deriveEncoder
  implicit val cardanoBlockIdDecoder: Decoder[CardanoBlockId] = deriveDecoder
}
