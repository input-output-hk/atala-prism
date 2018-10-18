package io.iohk.cef.test
import java.nio.ByteBuffer

import akka.util.ByteString
import io.iohk.cef.codecs.nio.NioDecoder
import io.iohk.cef.crypto._
import io.iohk.cef.data.{DataItem, DataItemError}
import io.iohk.cef.ledger.ByteStringSerializable

case class TestDataItemError(something: Int) extends DataItemError

case class DummyInvalidDataItem(error: TestDataItemError, owners: Seq[SigningPublicKey], witnesses: Seq[(SigningPublicKey, Signature)]) extends DataItem {

  override def apply(): Either[DataItemError, Unit] = Left(error)
}

object DummyInvalidDataItem {
  private val encoder = implicitly[NioEncoder[DummyInvalidDataItem]]
  private val decoder = implicitly[NioDecoder[DummyInvalidDataItem]]
  implicit val serializable: ByteStringSerializable[DummyInvalidDataItem] = new ByteStringSerializable[DummyInvalidDataItem] {
    override def encode(t: DummyInvalidDataItem): ByteString = ByteString(encoder.encode(t))
    override def decode(u: ByteString): Option[DummyInvalidDataItem] = decoder.decode(ByteBuffer.wrap(u.toArray))
  }
}

case class DummyValidDataItem(owners: Seq[SigningPublicKey], witnesses: Seq[(SigningPublicKey, Signature)]) extends DataItem {

  override def apply(): Either[DataItemError, Unit] = Right(())
}

object DummyValidDataItem {
  private val encoder = implicitly[NioEncoder[DummyValidDataItem]]
  private val decoder = implicitly[NioDecoder[DummyValidDataItem]]
  implicit val dummyValidDataItemSerializable = new ByteStringSerializable[DummyValidDataItem] {
    override def decode(u: ByteString): Option[DummyValidDataItem] = decoder.decode(ByteBuffer.wrap(u.toArray))
    override def encode(t: DummyValidDataItem): ByteString = ByteString(encoder.encode(t))
  }
}
