package io.iohk.cef.core
import akka.util.ByteString
import io.iohk.cef.ledger.ByteStringSerializable
import io.iohk.cef.network.NetworkError

import scala.concurrent.Future

trait NetworkComponent[State] {

  def disseminate[T](envelope: Envelope[T])(
      implicit serializable: ByteStringSerializable[Envelope[T]]): Future[Either[NetworkError, Unit]]

  def receive[T](byteString: ByteString)(
      implicit serializable: ByteStringSerializable[Envelope[T]]): Future[Either[NetworkError, Envelope[T]]]
}
