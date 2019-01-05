package io.iohk.cef.data

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data.DataItemAction._
import io.iohk.cef.data.query.QueryEngine
import io.iohk.cef.network.{MessageStream, Network}
import io.iohk.cef.transactionservice.{Envelope, Everyone}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._
import org.scalatest.mockito.MockitoSugar._

import scala.concurrent.Future

class DataItemServiceSpec extends FlatSpec {

  private val table = mock[Table[String]]
  private implicit val canValidate: CanValidate[DataItem[String]] = _ => Right(())

  private val defaultKeys = generateSigningKeyPair()
  private val defaultItemId = "id"
  private val defaultItemData = "foo"
  private val defaultSignature = sign(LabeledItem.Create(defaultItemData), defaultKeys.`private`)
  private val defaultOwner = Owner(defaultKeys.public, defaultSignature)
  private val dataItem: DataItem[String] = DataItem(defaultItemId, defaultItemData, Seq(), NonEmptyList(defaultOwner))
  private val containerId = "container-id"

  behavior of "DataItemService"

  it should "insert a data item" in {
    val network = mock[Network[Envelope[DataItemAction[String]]]]
    val messageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]
    when(network.messageStream).thenReturn(messageStream)
    when(messageStream.foreach(any())).thenReturn(Future.successful(()))
    val service: DataItemService[String] = new DataItemService(table, network, mock[QueryEngine[String]])
    when(table.insert(any())(any())).thenReturn(Right(()))

    service.processAction(Envelope(InsertAction(dataItem), containerId, Everyone))

    verify(table).insert(dataItem)
  }

  it should "validate a data item" in {
    val network = mock[Network[Envelope[DataItemAction[String]]]]
    val messageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]
    when(network.messageStream).thenReturn(messageStream)
    when(messageStream.foreach(any())).thenReturn(Future.successful(()))
    val service: DataItemService[String] = new DataItemService(table, network, mock[QueryEngine[String]])

    when(table.validate(any())(any())).thenReturn(true)

    service.processAction(Envelope(ValidateAction(dataItem), containerId, Everyone)) must ===(
      Right(DataItemServiceResponse.Validation(true)))

    when(table.validate(any())(any())).thenReturn(false)

    service.processAction(Envelope(ValidateAction(dataItem), containerId, Everyone)) must ===(
      Right(DataItemServiceResponse.Validation(false)))

  }

  it should "delete a data item" in {
    val signature = mock[Signature]
    val network = mock[Network[Envelope[DataItemAction[String]]]]
    val messageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]
    when(network.messageStream).thenReturn(messageStream)
    when(messageStream.foreach(any())).thenReturn(Future.successful(()))
    val service: DataItemService[String] = new DataItemService(table, network, mock[QueryEngine[String]])
    when(table.delete(any(), any())(any())).thenReturn(Right(()))

    service.processAction(Envelope(DeleteAction(dataItem.id, signature), containerId, Everyone))

    verify(table).delete(dataItem.id, signature)
  }
}
