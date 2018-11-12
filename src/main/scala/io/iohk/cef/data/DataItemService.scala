package io.iohk.cef.data

import io.iohk.cef.codecs.nio._
import io.iohk.cef.core.{DestinationDescriptor, Envelope}
import io.iohk.cef.data.DataItemAction.{Delete, Insert}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network}

class DataItemService[T](table: Table, network: Network[Envelope[DataItemAction[T]]])(
    implicit enc: NioEncDec[T],
    actionEncDec: NioEncDec[DeleteSignatureWrapper[T]],
    destinationDescriptorEncDec: NioEncDec[DestinationDescriptor],
    itemEncDec: NioEncDec[DataItem[T]],
    canValidate: CanValidate[DataItem[T]],
    frameCodec: NioEncDec[Envelope[DataItemAction[T]]]) {

  private val inboundMessages: MessageStream[Envelope[DataItemAction[T]]] = network.messageStream

  inboundMessages.foreach(handleMessage)

  def processAction(envelope: Envelope[DataItemAction[T]]): Either[ApplicationError, Unit] = {
    network.disseminateMessage(envelope)
    handleMessage(envelope)
  }

  private def handleMessage(message: Envelope[DataItemAction[T]]): Either[ApplicationError, Unit] = message match {
    case Envelope(Insert(dataItem), containerId, _) =>
      table.insert(containerId, dataItem)
    case Envelope(Delete(dataItemId, signature), containerId, _) =>
      table.delete[T](containerId, dataItemId, signature)
    case _ =>
      throw new IllegalStateException("Unexpected data item message '$message'.")
  }
}
