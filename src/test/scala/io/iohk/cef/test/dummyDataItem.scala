package io.iohk.cef.test
import java.nio.ByteBuffer

import akka.util.ByteString
import io.iohk.cef.codecs.nio.NioDecoder
import io.iohk.cef.crypto._
import io.iohk.cef.data.{DataItem, DataItemError, Owner, Witness}
import io.iohk.cef.ledger.ByteStringSerializable

case class TestDataItemError(something: Int) extends DataItemError

case class DummyInvalidDataItem(
    id: String,
    data: String,
    error: TestDataItemError,
    owners: Seq[Owner],
    witnesses: Seq[Witness])
    extends DataItem[String] {

  override def apply(): Either[DataItemError, Unit] = Left(error)
}

object DummyDataItemImplicits {
  private val encoder = implicitly[NioEncoder[String]]
  private val decoder = implicitly[NioDecoder[String]]
  implicit val serializable: ByteStringSerializable[String] = new ByteStringSerializable[String] {
    override def encode(t: String): ByteString = ByteString(encoder.encode(t))
    override def decode(u: ByteString): Option[String] = decoder.decode(ByteBuffer.wrap(u.toArray))
  }
}

case class DummyValidDataItem(id: String, data: String, owners: Seq[Owner], witnesses: Seq[Witness])
    extends DataItem[String] {

  override def apply(): Either[DataItemError, Unit] = Right(())
}
