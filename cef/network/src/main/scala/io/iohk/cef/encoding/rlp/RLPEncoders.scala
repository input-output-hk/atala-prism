package io.iohk.cef.encoding.rlp

import akka.util.ByteString
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.encoding.rlp.RLPImplicits._

// Adapt concrete RLP encoding scheme to generic cef encoders.
trait RLPEncoders {

  type E[T] = Encoder[T, RLPEncodeable]
  type D[U] = Decoder[RLPEncodeable, U]

  implicit val byteEnc: E[Byte] = byteEncDec.encode(_)
  implicit val byteDec: D[Byte] = byteEncDec.decode(_)

  implicit val shortEnc: E[Short] = shortEncDec.encode(_)
  implicit val shortDec: D[Short] = shortEncDec.decode(_)

  implicit val intEnc: E[Int] = intEncDec.encode(_)
  implicit val intDec: D[Int] = intEncDec.decode(_)

  implicit val bigIntEnc: E[BigInt] = bigIntEncDec.encode(_)
  implicit val bigIntDec: D[BigInt] = bigIntEncDec.decode(_)

  implicit val longEnc: E[Long] = longEncDec.encode(_)
  implicit val longDec: D[Long] = longEncDec.decode(_)

  implicit val stringEnc: E[String] = stringEncDec.encode(_)
  implicit val stringDec: D[String] = stringEncDec.decode(_)

  implicit val byteArrayEnc: E[Array[Byte]] = byteArrayEncDec.encode(_)
  implicit val byteArrayDec: D[Array[Byte]] = byteArrayEncDec.decode(_)

  implicit val byteStringEnc: E[ByteString] = byteStringEncDec.encode(_)
  implicit val byteStringDec: D[ByteString] = byteStringEncDec.decode(_)

  implicit def seqEnc[T: RLPEncoder : RLPDecoder]: E[Seq[T]] = seqEncDec[T]().encode(_)
  implicit def seqDec[T: RLPEncoder : RLPDecoder]: Decoder[RLPEncodeable, Seq[T]] = seqEncDec[T]().decode(_)

}

object RLPEncoders extends RLPEncoders
