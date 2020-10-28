package io.iohk.atala.prism.node.cardano.models

import io.circe.{ACursor, Json}
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.util.BytesOps

import scala.util.Try

object AtalaObjectMetadata {
  // Last 16 bits of 344977920845, which is the decimal representation of the concatenation of the hexadecimal values
  // (50 52 49 53 4d) of the word PRISM in ASCII.
  val METADATA_PRISM_INDEX = 21325

  private val VERSION_KEY = "v"
  private val CONTENT_KEY = "c"
  // Key to denote that a JSON object actually represents a string of bytes in hexadecimal format
  // (used by Cardano DB Sync)
  private val BYTE_STRING_KEY = "hex"
  // Prefix to denote that the following characters represent a string of bytes in hexadecimal format
  // (needed by Cardano Wallet)
  private val BYTE_STRING_PREFIX = "0x"
  // Maximum number of bytes that can be represented by a byte string (enforced by Cardano Node)
  private val BYTE_STRING_LIMIT = 64

  def fromTransactionMetadata(metadata: TransactionMetadata): Option[node_internal.AtalaObject] = {
    val prismMetadata = metadata.json.hcursor
      .downField(METADATA_PRISM_INDEX.toString)

    prismMetadata
      .downField(VERSION_KEY)
      .focus
      .flatMap(_.asNumber)
      .flatMap(_.toInt)
      .find(_ == 1)
      .flatMap(_ => fromTransactionMetadataV1(prismMetadata))
  }

  private def fromTransactionMetadataV1(prismMetadata: ACursor): Option[node_internal.AtalaObject] = {
    val bytes = prismMetadata
      .downField(CONTENT_KEY)
      .focus
      .flatMap(_.asArray)
      .getOrElse(Vector[Json]())
      .flatMap(parseByteString)
      .toArray
    if (bytes.isEmpty) {
      // Either the content does not exist, is not the right type, or is truly empty
      None
    } else {
      node_internal.AtalaObject.validate(bytes).toOption
    }
  }

  private def parseByteString(byteString: Json): Array[Byte] = {
    byteString.hcursor
      .downField(BYTE_STRING_KEY)
      .focus
      .flatMap(_.asString)
      .map(hex => Try(BytesOps.hexToBytes(hex)).getOrElse(Array()))
      .getOrElse(Array())
  }

  def toTransactionMetadata(atalaObject: node_internal.AtalaObject): TransactionMetadata = {
    TransactionMetadata(
      Json.obj(
        METADATA_PRISM_INDEX.toString -> Json.obj(
          VERSION_KEY -> Json.fromInt(1),
          CONTENT_KEY -> Json.arr(
            atalaObject.toByteArray
              .grouped(BYTE_STRING_LIMIT)
              .map(bytes => Json.fromString(toByteString(bytes)))
              .toSeq: _*
          )
        )
      )
    )
  }

  private def toByteString(bytes: Array[Byte]): String = {
    BYTE_STRING_PREFIX + BytesOps.bytesToHex(bytes.toIndexedSeq)
  }
}
