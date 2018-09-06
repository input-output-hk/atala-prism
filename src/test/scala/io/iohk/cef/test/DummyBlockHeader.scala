package io.iohk.cef.test
import akka.util.ByteString
import io.iohk.cef.ledger.{BlockHeader, ByteStringSerializable}
import io.iohk.cef.utils.ByteSizeable

case class DummyBlockHeader(val sizeInBytes: Int) extends BlockHeader {
  override def toString: String = s"DummyBlockHeader($sizeInBytes)"
}

object DummyBlockHeader {
  implicit val sizeable = new ByteSizeable[DummyBlockHeader] {
    override def sizeInBytes(t: DummyBlockHeader): Int = t.sizeInBytes
  }

  implicit val serializable = new ByteStringSerializable[DummyBlockHeader] {
    override def deserialize(bytes: ByteString): DummyBlockHeader = DummyBlockHeader(BigInt(bytes.toArray).intValue())

    override def serialize(t: DummyBlockHeader): ByteString = ByteString(BigInt(t.sizeInBytes).toByteArray)
  }
}
