package io.iohk.cef.network
import akka.util.ByteString
import io.iohk.cef.core.Envelope
import io.iohk.cef.ledger.{Block, ByteStringSerializable, Transaction}

trait NetworkComponent[F[_], State] {

  def disseminate[T](envelope: Envelope[T])(implicit serializable: ByteStringSerializable[Envelope[T]]): F[Either[NetworkError, Unit]]

  def receive[T](byteString: ByteString)(implicit serializable: ByteStringSerializable[Envelope[T]]): F[Either[NetworkError, Envelope[T]]]
}
