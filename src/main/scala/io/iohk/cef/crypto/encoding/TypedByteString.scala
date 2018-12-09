package io.iohk.cef.crypto.encoding

import akka.util.ByteString
import io.iohk.cef.utils._
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._

case class TypedByteString(`type`: String, bytes: ByteString) {

  import TypedByteString._

  def toByteString: ByteString = {
    TypedByteStringNioCodec.encode(this).toByteString
  }

  /**
    * {{{
    *
    * >>> import akka.util.ByteString
    * >>> TypedByteString("ABC", ByteString("ABC"))
    * -----BEGIN TYPED BYTE STRING ABC BLOCK-----
    *  00 00 00 82 00 00 00 10 A7 F1 59 E6 70 8B 88 34
    *  57 37 55 A6 91 9F 54 68 00 00 00 33 00 00 00 10
    *  B2 45 CF DD AA 64 58 7E 8F 28 A2 A3 F9 FD EF 2D
    *  00 00 00 1B 00 00 00 10 CB D3 1B A7 45 A2 FA D4
    *  46 EE F1 19 99 61 2C E7 00 00 00 03 41 42 43 00
    *  00 00 33 00 00 00 10 B4 16 0B AA E3 62 AD 83 3D
    *  AE 50 13 56 1B 64 A8 00 00 00 1B 00 00 00 10 CB
    *  D3 1B A7 45 A2 FA D4 46 EE F1 19 99 61 2C E7 00
    *  00 00 03 41 42 43
    * -----END TYPED BYTE STRING ABC BLOCK-----
    *
    * }}}
    */
  override def toString: String =
    toString("typed byte string")

  def toString(title: String): String = {
    val full = s"$title ${`type`}".toUpperCase
    s"""|-----BEGIN $full BLOCK-----
        |${toByteString.toHex}
        |-----END $full BLOCK-----""".stripMargin
  }

}

object TypedByteString {

  private[crypto] val TypedByteStringNioCodec: NioCodec[TypedByteString] = NioCodec[TypedByteString]

  private implicit val byteOrder: java.nio.ByteOrder = java.nio.ByteOrder.BIG_ENDIAN

  def decodeFrom(bytes: ByteString): Either[TypedByteStringDecodingError, TypedByteString] = {
    TypedByteStringNioCodec.decode(bytes.toByteBuffer) match {
      case Some(tbs) => Right(tbs)
      case None => Left(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
    }
  }

  /**
    * {{{
    *
    * >>> import akka.util.ByteString
    * >>> val text: String =
    * ...   """|-----BEGIN TYPED BYTE STRING ABC BLOCK-----
    * ...      | 00 00 00 82 00 00 00 10 A7 F1 59 E6 70 8B 88 34
    * ...      | 57 37 55 A6 91 9F 54 68 00 00 00 33 00 00 00 10
    * ...      | B2 45 CF DD AA 64 58 7E 8F 28 A2 A3 F9 FD EF 2D
    * ...      | 00 00 00 1B 00 00 00 10 CB D3 1B A7 45 A2 FA D4
    * ...      | 46 EE F1 19 99 61 2C E7 00 00 00 03 41 42 43 00
    * ...      | 00 00 33 00 00 00 10 B4 16 0B AA E3 62 AD 83 3D
    * ...      | AE 50 13 56 1B 64 A8 00 00 00 1B 00 00 00 10 CB
    * ...      | D3 1B A7 45 A2 FA D4 46 EE F1 19 99 61 2C E7 00
    * ...      | 00 00 03 41 42 43
    * ...      |-----END TYPED BYTE STRING ABC BLOCK-----""".stripMargin
    * >>> TypedByteString.parseFrom(text) == Right(TypedByteString("ABC", ByteString("ABC")))
    * true
    *
    * >>> TypedByteString.parseFrom(
    * ...   """|-----BEGIN TYPED BYTE STRING BLOCK-----
    * ...      | 12 3
    * ...      |-----END TYPED BYTE STRING BLOCK-----""".stripMargin)
    * Left(CorruptedSourceText)
    *
    * >>> TypedByteString.parseFrom(
    * ...   """|-----BEGIN TYPED BYTE STRING BLOCK-----
    * ...      | 12 3X
    * ...      |-----END TYPED BYTE STRING BLOCK-----""".stripMargin)
    * Left(NumberFormatError(java.lang.NumberFormatException: For input string: "3X"))
    *
    * >>> val corruptedText: String =
    * ...   """|-----BEGIN TYPED BYTE STRING BLOCK-----
    * ...      | 00 0A 00 3D 00 00 00 10 A7 F1 59 E6 70 8B 88 34
    * ...      | 57 37 55 A6 91 9F 54 68 00 00 00 03 00 41 00 42
    * ...      | 00 43 00 00 00 1B 00 00 00 10 5E 2F 6E 52 FA CE
    * ...      | CC 9B 9F 82 B6 38 B5 13 00 C2 00 00 00 03 41 42
    * ...      | 43
    * ...      |-----END TYPED BYTE STRING BLOCK-----""".stripMargin
    * >>> TypedByteString.parseFrom(corruptedText)
    * Left(DecodingError(NioDecoderFailedToDecodeTBS))
    *
    * }}}
    */
  def parseFrom(text: String): Either[TypedByteStringParsingError, TypedByteString] = {
    def removeComment(line: String): String =
      line.indexOf("--") match {
        case -1 => line
        case i => line.take(i)
      }

    val plainHex =
      text
        .split("\n")
        .toList
        .map(removeComment)
        .mkString
        .filterNot(_.isWhitespace)

    if ((plainHex.length % 2) == 0) {
      val builder = ByteString.newBuilder
      builder.sizeHint(plainHex.length / 2)
      plainHex.grouped(2).foreach { byteString =>
        try {
          builder += Integer.parseInt(byteString, 16).toByte
        } catch {
          case e: java.lang.NumberFormatException =>
            return Left(TypedByteStringParsingError.NumberFormatError(e))
        }
      }
      TypedByteString
        .decodeFrom(builder.result)
        .left
        .map(TypedByteStringParsingError.DecodingError.apply)
    } else
      Left(TypedByteStringParsingError.CorruptedSourceText)
  }
}

sealed trait TypedByteStringDecodingError
object TypedByteStringDecodingError {
  case object NioDecoderFailedToDecodeTBS extends TypedByteStringDecodingError
}

sealed trait TypedByteStringParsingError
object TypedByteStringParsingError {
  case object CorruptedSourceText extends TypedByteStringParsingError
  case class DecodingError(e: TypedByteStringDecodingError) extends TypedByteStringParsingError
  case class NumberFormatError(e: java.lang.NumberFormatException) extends TypedByteStringParsingError
}
