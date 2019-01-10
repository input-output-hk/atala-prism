package io.iohk.cef.data

import io.iohk.cef.codecs.nio._
import io.iohk.cef.transactionservice.Envelope
import io.iohk.cef.data.DataItemAction._
import io.iohk.cef.data.query.{DataItemQuery, DataItemQueryEngine}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network}
import io.iohk.cef.data.DataItemAction.{DeleteAction, InsertAction}
import io.iohk.cef.data.query.{DataItemQuery, DataItemQueryEngine}
import scala.reflect.runtime.universe.TypeTag

class DataItemService[T](
    table: Table[T],
    network: Network[Envelope[DataItemAction[T]]],
    queryEngine: DataItemQueryEngine[T]
)(implicit enc: NioCodec[T], typeTag: TypeTag[T], canValidate: CanValidate[DataItem[T]]) {

  private val inboundMessages: MessageStream[Envelope[DataItemAction[T]]] = network.messageStream

  inboundMessages.foreach(handleMessage)

  def processAction(envelope: Envelope[DataItemAction[T]]): Either[ApplicationError, DataItemServiceResponse] = {
    network.disseminateMessage(envelope)
    handleMessage(envelope)
  }

  def processQuery(envelope: Envelope[DataItemQuery]): MessageStream[Either[ApplicationError, Seq[DataItem[T]]]] =
    queryEngine.process(envelope.content)

  private def handleMessage(message: Envelope[DataItemAction[T]]): Either[ApplicationError, DataItemServiceResponse] =
    message match {
      case Envelope(InsertAction(dataItem), _, _) =>
        table.insert(dataItem).map(_ => DataItemServiceResponse.DIUnit)
      case Envelope(ValidateAction(dataItem), _, _) =>
        Right(DataItemServiceResponse.Validation(table.validate(dataItem)))
      case Envelope(DeleteAction(dataItemId, signature), _, _) =>
        table.delete(dataItemId, signature).map(_ => DataItemServiceResponse.DIUnit)
      case _ =>
        throw new IllegalStateException("Unexpected data item message '$message'.")
    }

}

sealed trait DataItemServiceResponse
object DataItemServiceResponse {
  case object DIUnit extends DataItemServiceResponse
  case class Validation(isValid: Boolean) extends DataItemServiceResponse
}
