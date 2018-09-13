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
      Try(DummyBlockHeader(BigInt(bytes.toArray).intValue())).toOption

    override def encode(t: DummyBlockHeader): ByteString = ByteString(BigInt(t.sizeInBytes).toByteArray)
  }
}
