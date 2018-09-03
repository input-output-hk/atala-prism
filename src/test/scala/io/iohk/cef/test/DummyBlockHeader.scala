package io.iohk.cef.test
import io.iohk.cef.ledger.BlockHeader
import io.iohk.cef.utils.ByteSizeable

case class DummyBlockHeader(val sizeInBytes: Int) extends BlockHeader {
  override def toString: String = s"DummyBlockHeader($sizeInBytes)"
}

object DummyBlockHeader {
  implicit val sizeable = new ByteSizeable[DummyBlockHeader] {
    override def sizeInBytes(t: DummyBlockHeader): Int = t.sizeInBytes
  }
}
