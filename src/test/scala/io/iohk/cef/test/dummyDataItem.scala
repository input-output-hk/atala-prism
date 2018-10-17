package io.iohk.cef.test
import akka.util.ByteString
import io.iohk.cef.data.{DataItem, DataItemError}
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.ByteStringSerializable

case class DummyInvalidDataItem(error: DataItemError, owners: Seq[SigningPublicKey], witnesses: Seq[(SigningPublicKey, Signature)]) extends DataItem {

  override def apply(): Either[DataItemError, Unit] = Left(error)
}

object DummyInvalidDataItem {
  implicit val dummyInvalidDataItemSerializable = new ByteStringSerializable[DummyInvalidDataItem] {
    override def decode(
        u: ByteString): Option[DummyInvalidDataItem] =
      ByteString("DummyValidDataItem") ++ 

    override def encode(t: DummyInvalidDataItem): ByteString = ???
  }
}

case class DummyValidDataItem(owners: Seq[SigningPublicKey], witnesses: Seq[(SigningPublicKey, Signature)]) extends DataItem {

  override def apply(): Either[DataItemError, Unit] = Right()
}

object DummyValidDataItem {
  implicit val dummyValidDataItemSerializable = new ByteStringSerializable[DummyValidDataItem] {
    override def decode(u: ByteString): Option[DummyValidDataItem] =
      ???

    override def encode(t: DummyValidDataItem): ByteString = ???
  }
}
