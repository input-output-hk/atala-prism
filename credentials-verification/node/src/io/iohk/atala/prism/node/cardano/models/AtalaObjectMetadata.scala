package io.iohk.atala.prism.node.cardano.models

import io.circe.Json
import io.iohk.prism.protos.node_internal

object AtalaObjectMetadata {
  // Last 16 bits of 344977920845, which is the decimal representation of the concatenation of the hexadecimal values
  // (50 52 49 53 4d) of the word PRISM in ASCII.
  val METADATA_PRISM_INDEX = 21325
  private[models] val METADATA_VERSION = 1

  private[models] val VERSION_KEY = "version"
  private[models] val CONTENT_KEY = "content"

  def fromTransactionMetadata(metadata: TransactionMetadata): Option[node_internal.AtalaObject] = {
    val prismMetadata = metadata.json.hcursor
      .downField(METADATA_PRISM_INDEX.toString)

    val version = prismMetadata.downField(VERSION_KEY).focus.flatMap(_.asNumber).flatMap(_.toInt).getOrElse(-1)
    if (version == METADATA_VERSION) {
      val bytes = prismMetadata
        .downField(CONTENT_KEY)
        .focus
        .flatMap(_.asArray)
        .getOrElse(Vector[Json]())
        .flatMap(_.asNumber)
        .flatMap(_.toByte)
        .toArray
      if (bytes.isEmpty) {
        // Either the content does not exist, is not the right type, or is truly empty
        None
      } else {
        node_internal.AtalaObject.validate(bytes).toOption
      }
    } else {
      None
    }
  }

  def toTransactionMetadata(atalaObject: node_internal.AtalaObject): TransactionMetadata = {
    TransactionMetadata(
      Json.obj(
        METADATA_PRISM_INDEX.toString -> Json.obj(
          VERSION_KEY -> Json.fromInt(METADATA_VERSION),
          CONTENT_KEY -> Json.arr(atalaObject.toByteArray.map(byte => Json.fromInt(byte.toInt)): _*)
        )
      )
    )
  }
}
