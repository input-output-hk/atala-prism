package io.iohk.cef.test
import java.nio.ByteBuffer

import akka.util.ByteString
import io.iohk.cef.codecs.nio.NioDecoder
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.ByteStringSerializable

case class TestDataItemError(something: Int) extends DataItemError

class InvalidValidation[T](something: Int) extends CanValidate[T] {
  override def validate(t: T): Either[ApplicationError, Unit] = Right(TestDataItemError(something))
}

class ValidValidation[T] extends CanValidate[T] {
  override def validate(t: T): Either[ApplicationError, Unit] = Right(())
}

object DummyDataItemImplicits {
  private val encoder = implicitly[NioEncoder[String]]
  private val decoder = implicitly[NioDecoder[String]]
  implicit val serializable: ByteStringSerializable[String] = new ByteStringSerializable[String] {
    override def encode(t: String): ByteString = ByteString(encoder.encode(t))
    override def decode(u: ByteString): Option[String] = decoder.decode(ByteBuffer.wrap(u.toArray))
  }
}
