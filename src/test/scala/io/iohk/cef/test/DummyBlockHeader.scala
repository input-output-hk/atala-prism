package io.iohk.cef.test
import io.iohk.cef.ledger.{BlockHeader}
import io.iohk.cef.codecs.nio._
import io.iohk.cef.utils._

import scala.util.Try
import java.nio.ByteBuffer

case class DummyBlockHeader(val sizeInBytes: Int) extends BlockHeader {
  override def toString: String = s"DummyBlockHeader($sizeInBytes)"
}

object DummyBlockHeader {

  implicit val sizeable: ByteSizeable[DummyBlockHeader] = new ByteSizeable[DummyBlockHeader] {
    override def sizeInBytes(t: DummyBlockHeader): Int = t.sizeInBytes
  }

  private def decode(bb: ByteBuffer): Option[DummyBlockHeader] = {
    val bytes = bb.toByteString
    Try(if (bytes.forall(_ == 2)) {
      DummyBlockHeader(bytes.size)
    } else throw new IllegalArgumentException("Invalid format for DummyBlockHeader")).toOption
  }

  private def encode(t: DummyBlockHeader): ByteBuffer =
    Array.fill[Byte](t.sizeInBytes)(2.toByte).toByteBuffer

  implicit val serializable: NioEncDec[DummyBlockHeader] = NioEncDec(encode _, decode _)
}
