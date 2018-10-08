package io.iohk.cef.crypto
package encoding

import akka.util.ByteString
import io.iohk.cef.utils._

case class TypedByteString(`type`: String, bytes: ByteString) {

  def toByteString: ByteString = {
    // FIXME: To implement directly, without using an external encoder
    //        Maybe use the same technic used by multiformats
    val nioEncoder: NioEncoder[TypedByteString] = implicitly
    ByteString(nioEncoder.encode(this))
  }

  /**
    * {{{
    *
    * >>> import akka.util.ByteString
    * >>> TypedByteString("ABC", ByteString("ABC"))
    * -----BEGIN TYPED BYTE STRING ABC BLOCK-----
    *  00 00 00 3D 00 00 00 10 A7 F1 59 E6 70 8B 88 34
    *  57 37 55 A6 91 9F 54 68 00 00 00 03 00 41 00 42
    *  00 43 00 00 00 1B 00 00 00 10 0E BB 60 87 0B 23
    *  D1 57 2A 23 C3 DB 6A AD 73 54 00 00 00 03 41 42
    *  43
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
  def decodeFrom(bytes: ByteString): Either[TypedByteStringDecodingError, TypedByteString] = {
    // FIXME: To implement directly, without using an external encoder
    //        Maybe use the same technic used by multiformats
    val nioDecoder: NioDecoder[TypedByteString] = implicitly
    nioDecoder.decode(bytes.toByteBuffer) match {
      case Some(entity) => Right(entity)
      case None => Left(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
    }
  }

  /**
    * {{{
    *
    * >>> import akka.util.ByteString
    * >>> val text: String =
    * ...   """|-----BEGIN TYPED BYTE STRING ABC BLOCK-----
    * ...      | 00 00 00 3D 00 00 00 10 A7 F1 59 E6 70 8B 88 34
    * ...      | 57 37 55 A6 91 9F 54 68 00 00 00 03 00 41 00 42
    * ...      | 00 43 00 00 00 1B 00 00 00 10 0E BB 60 87 0B 23
    * ...      | D1 57 2A 23 C3 DB 6A AD 73 54 00 00 00 03 41 42
    * ...      | 43
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
