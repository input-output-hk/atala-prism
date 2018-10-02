package io.iohk.cef.test
import akka.util.ByteString
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable}
import io.iohk.cef.utils.ByteSizeable

import scala.util.Try

case class DummyBlockHeader(val sizeInBytes: Int) extends BlockHeader {
  override def toString: String = s"DummyBlockHeader($sizeInBytes)"
}

object DummyBlockHeader {
  implicit val sizeable = new ByteSizeable[DummyBlockHeader] {
    override def sizeInBytes(t: DummyBlockHeader): Int = t.sizeInBytes
  }

  implicit val serializable = new ByteStringSerializable[DummyBlockHeader] {
    override def decode(bytes: ByteString): Option[DummyBlockHeader] =
      Try(if (bytes.forall(_ == 2)) {
        DummyBlockHeader(bytes.size)
      } else throw new IllegalArgumentException("Invalid format for DummyBlockHeader")).toOption

    override def encode(t: DummyBlockHeader): ByteString =
      ByteString(Array.fill[Byte](t.sizeInBytes)(2))
  }
}
