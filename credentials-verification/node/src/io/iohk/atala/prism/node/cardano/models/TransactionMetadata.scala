package io.iohk.atala.prism.node.cardano.models

import io.circe.Json
import io.iohk.prism.protos.node_internal

case class TransactionMetadata(json: Json)

object TransactionMetadata {
  // Last 16 bits of 344977920845, which is the decimal representation of the concatenation of the hexadecimal values
  // (50 52 49 53 4d) of the word PRISM in ASCII.
  private val METADATA_PRISM_INDEX = 21325
  private val METADATA_VERSION = 1

  def fromProto(atalaObject: node_internal.AtalaObject): TransactionMetadata = {
    TransactionMetadata(
      Json.obj(
        s"$METADATA_PRISM_INDEX" -> Json.obj(
          "version" -> Json.fromInt(METADATA_VERSION),
          "content" -> Json.arr(atalaObject.toByteArray.map(byte => Json.fromInt(byte.toInt)): _*)
        )
      )
    )
  }
}
