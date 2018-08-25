package io.iohk.cef.network
import akka.util.ByteString
import io.iohk.cef.core.Envelope
import io.iohk.cef.ledger.ByteStringSerializable

import scala.concurrent.Future

trait NetworkComponent[State] {

  def disseminate[T](envelope: Envelope[T])(implicit serializable: ByteStringSerializable[Envelope[T]]): Future[Either[NetworkError, Unit]]

  def receive[T](byteString: ByteString)(implicit serializable: ByteStringSerializable[Envelope[T]]): Future[Either[NetworkError, Envelope[T]]]
}
