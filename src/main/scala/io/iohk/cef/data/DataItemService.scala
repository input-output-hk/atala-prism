package io.iohk.cef.data

import io.iohk.cef.crypto.Signature
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network}
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.core.{DestinationDescriptor, Envelope}
import io.iohk.cef.data.DataItemAction.{Delete, Insert}
import io.iohk.cef.network.discovery.NetworkDiscovery

class DataItemService[T](table: Table, transports: Transports, networkDiscovery: NetworkDiscovery)(
    implicit enc: NioEncDec[T],
    actionEncDec: NioEncDec[DataItemAction[T]],
    destinationDescriptorEncDec: NioEncDec[DestinationDescriptor],
    itemEncDec: NioEncDec[DataItem[T]],
    canValidate: CanValidate[DataItem[T]]) {

  private val network = new Network[Envelope[DataItemAction[T]]](networkDiscovery, transports)

  private val inboundMessages: MessageStream[Envelope[DataItemAction[T]]] = network.messageStream

  inboundMessages.foreach(handleMessage)

  def insert(envelope: Envelope[DataItem[T]]): Either[ApplicationError, Unit] = {
    val envelopeAction = envelope.map(Insert.apply)
    network.disseminateMessage(envelopeAction)
    handleMessage(envelopeAction)
  }

  def delete(envelope: Envelope[DataItem[T]], deleteSignature: Signature): Either[ApplicationError, Unit] = {
    val envelopeAction = envelope.map(x => Delete.apply(x, deleteSignature))
    network.disseminateMessage(envelopeAction)
    handleMessage(envelopeAction)
  }

  private def handleMessage(message: Envelope[DataItemAction[T]]): Either[ApplicationError, Unit] = message match {
    case Envelope(Insert(dataItem), containerId, _) =>
      table.insert(containerId, dataItem)
    case Envelope(Delete(dataItem, deleteSignature), containerId, _) =>
      table.delete(containerId, dataItem, deleteSignature)
    case _ =>
      throw new IllegalStateException("Unexpected data item message '$message'.")
  }
}
