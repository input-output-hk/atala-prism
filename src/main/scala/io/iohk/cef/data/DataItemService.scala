package io.iohk.cef.data

import io.iohk.cef.crypto.Signature
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network}
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.codecs.nio._
import io.iohk.cef.core.{DestinationDescriptor, Envelope}
import io.iohk.cef.data.DataItemAction.{Delete, Insert}
import io.iohk.cef.network.discovery.NetworkDiscovery

class DataItemService[T](table: Table, transports: Transports, networkDiscovery: NetworkDiscovery)(
    implicit enc: NioEncoder[T],
    dec: NioDecoder[T]) {

  private implicit val actionEnc: NioEncoder[DataItemAction[T]] = NioEncoder[DataItemAction[T]]
  private implicit val destEnc: NioEncoder[DestinationDescriptor] = NioEncoder[DestinationDescriptor]

  private implicit val actionDec: NioDecoder[DataItemAction[T]] = NioDecoder[DataItemAction[T]]
  private implicit val destDec: NioDecoder[DestinationDescriptor] = NioDecoder[DestinationDescriptor]

  private val network = new Network[Envelope[DataItemAction[T]]](networkDiscovery, transports)

  private val inboundMessages: MessageStream[Envelope[DataItemAction[T]]] = network.messageStream

  inboundMessages.foreach(handleMessage)

  def processDataItem(envelopeAction: Envelope[DataItemAction[T]]): Unit =
    network.disseminateMessage(envelopeAction)

  private def handleMessage(message: Envelope[DataItemAction[T]]): Unit = message match {
    case Envelope(Insert(dataItem), id, destinationDescriptor) =>
      insert(dataItem)
    case Envelope(Delete(dataItem, deleteSignature), id, destinationDescriptor) =>
      delete(dataItem, deleteSignature)
    case _ =>
      throw new IllegalStateException("Unexpected data item message '$message'.")
  }

  private def insert(dataItem: DataItem[T]): Either[ApplicationError, Unit] = {
    table.insert(dataItem)
  }

  private def delete(dataItem: DataItem[T], deleteSignature: Signature): Either[ApplicationError, Unit] =
    table.delete(dataItem, deleteSignature)
}
