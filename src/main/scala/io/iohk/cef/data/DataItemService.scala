package io.iohk.cef.data

import io.iohk.cef.codecs.nio._
import io.iohk.cef.transactionservice.Envelope
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network}
import io.iohk.cef.data.DataItemAction.{DeleteAction, InsertAction}
import io.iohk.cef.data.query.{Query, QueryEngine}
import scala.reflect.runtime.universe.TypeTag

class DataItemService[T](table: Table[T], network: Network[Envelope[DataItemAction[T]]], queryEngine: QueryEngine[T])(
    implicit enc: NioEncDec[T],
    typeTag: TypeTag[T],
    canValidate: CanValidate[DataItem[T]]) {

  private val inboundMessages: MessageStream[Envelope[DataItemAction[T]]] = network.messageStream

  inboundMessages.foreach(handleMessage)

  def processAction(envelope: Envelope[DataItemAction[T]]): Either[ApplicationError, Unit] = {
    network.disseminateMessage(envelope)
    handleMessage(envelope)
  }

  def processQuery(envelope: Envelope[Query]): MessageStream[Either[ApplicationError, Seq[DataItem[T]]]] =
    queryEngine.process(envelope.content)

  private def handleMessage(message: Envelope[DataItemAction[T]]): Either[ApplicationError, Unit] = message match {
    case Envelope(InsertAction(dataItem), _, _) =>
      table.insert(dataItem)
    case Envelope(DeleteAction(dataItemId, signature), containerId, _) =>
      table.delete(dataItemId, signature)
    case _ =>
      throw new IllegalStateException(s"Unexpected data item message '$message'.")
  }
}
