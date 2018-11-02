package io.iohk.cef.crypto.encoding

import akka.util.ByteString
import io.iohk.cef.utils._

case class TypedByteString(`type`: String, bytes: ByteString) {

  import TypedByteString._

  def toByteString: ByteString = {
    val typeBytes = `type`.getBytes("UTF-8")
    val size = typeBytes.length + bytes.length + 8
    val builder = ByteString.newBuilder
    builder.sizeHint(size)
    builder.putInt(typeBytes.length)
    builder ++= typeBytes
    builder.putInt(bytes.length)
    builder ++= bytes
    builder.result
  }

  /**
    * {{{
    *
    * >>> import akka.util.ByteString
    * >>> TypedByteString("ABC", ByteString("ABC"))
    * -----BEGIN TYPED BYTE STRING ABC BLOCK-----
    *  00 00 00 03 41 42 43 00 00 00 03 41 42 43
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

  private implicit val byteOrder: java.nio.ByteOrder = java.nio.ByteOrder.BIG_ENDIAN

  def decodeFrom(bytes: ByteString): Either[TypedByteStringDecodingError, TypedByteString] = {
    import java.nio.ByteBuffer
    val b = ByteBuffer.wrap(bytes.toArray).order(byteOrder)

    type E[T] = Either[TypedByteStringDecodingError, T]

    def readInt(): E[Int] =
      if (b.remaining < 4) {
        Left(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
      } else Right(b.getInt)

    def readNat(): E[Int] =
      readInt.flatMap {
        case i if i >= 0 => Right(i)
        case _ =>
          Left(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
      }

    def readArray(size: Int): E[Array[Byte]] =
      if (b.remaining < size) {
        Left(TypedByteStringDecodingError.NioDecoderFailedToDecodeTBS)
      } else {
        val array = new Array[Byte](size)
        b.get(array)
        Right(array)
      }

    def readFullArray(): E[Array[Byte]] =
      for {
        size <- readNat
        r <- readArray(size)
      } yield r

    def readString: E[String] =
      readFullArray().map(bs => new String(bs, "UTF-8"))

    b.position(0)

    for {
      `type` <- readString
      bytes <- readFullArray
    } yield {
      TypedByteString(`type`, ByteString(bytes))
    }

  }

  /**
    * {{{
    *
    * >>> import akka.util.ByteString
    * >>> val text: String =
    * ...   """|-----BEGIN TYPED BYTE STRING ABC BLOCK-----
    * ...      |  00 00 00 03 41 42 43 00 00 00 03 41 42 43
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
