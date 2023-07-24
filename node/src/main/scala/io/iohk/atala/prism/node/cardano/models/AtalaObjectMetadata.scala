package io.iohk.atala.prism.node.cardano.models

import io.circe.{ACursor, Json}
import io.iohk.atala.prism.protos.node_internal
import io.iohk.atala.prism.utils.BytesOps

import scala.util.Try

object AtalaObjectMetadata {
  // Last 16 bits of 344977920845, which is the decimal representation of the concatenation of the hexadecimal values
  // (50 52 49 53 4d) of the word PRISM in ASCII.
  val METADATA_PRISM_INDEX = 21325

  private val VERSION_KEY = "v"
  private val CONTENT_KEY = "c"
  // Prefix to denote that the following characters represent a string of bytes in hexadecimal format
  // (needed by Cardano Wallet)
  private val BYTE_STRING_PREFIX = "0x"
  // Maximum number of bytes that can be represented by a byte string (enforced by Cardano Node)
  private val BYTE_STRING_LIMIT = 64

  private val MAP_KEY = "k"
  private val MAP_VALUE = "v"
  private val MAP_TYPE = "map"
  private val LIST_TYPE = "list"
  private val INT_TYPE = "int"
  private val STRING_TYPE = "string"
  private val BYTES_TYPE = "bytes"

  def fromTransactionMetadata(
      metadata: TransactionMetadata
  ): Option[node_internal.AtalaObject] = {
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

  private def fromTransactionMetadataV1(
      prismMetadata: ACursor
  ): Option[node_internal.AtalaObject] = {
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
    byteString.asString
      .map(_.stripPrefix(BYTE_STRING_PREFIX))
      .map(hex => Try(BytesOps.hexToBytes(hex)).getOrElse(Array()))
      .getOrElse(Array())
  }

  def toTransactionMetadata(
      atalaObject: node_internal.AtalaObject
  ): TransactionMetadata = {
    TransactionMetadata(
      Json.obj(
        METADATA_PRISM_INDEX.toString -> Json.obj(
          MAP_TYPE -> Json.arr(
            Json.obj(
              MAP_KEY -> Json.obj(STRING_TYPE -> Json.fromString(VERSION_KEY)),
              MAP_VALUE -> Json.obj(INT_TYPE -> Json.fromInt(1))
            ),
            Json.obj(
              MAP_KEY -> Json.obj(STRING_TYPE -> Json.fromString(CONTENT_KEY)),
              MAP_VALUE -> Json.obj(
                LIST_TYPE -> Json.arr(
                  atalaObject.toByteArray
                    .grouped(BYTE_STRING_LIMIT)
                    .map(bytes =>
                      Json.obj(
                        BYTES_TYPE -> Json.fromString(
                          BytesOps.bytesToHex(bytes)
                        )
                      )
                    )
                    .toSeq: _*
                )
              )
            )
          )
        )
      )
    )
  }

  def estimateTxMetadataSize(atalaObject: node_internal.AtalaObject): Int = {
    toTransactionMetadata(atalaObject).json.noSpaces.length
  }
}
