package io.iohk.cef.data

import io.iohk.cef.codecs.nio._
import io.iohk.cef.core.{Envelope, Everyone}
import io.iohk.cef.crypto.{Signature, _}
import io.iohk.cef.data.DataItemAction.{Delete, Insert}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.transport.{Frame, FrameHeader}
import io.iohk.cef.network.{MessageStream, Network, NodeId}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class DataItemServiceSpec extends FlatSpec with MockitoSugar {

  private val table = mock[Table]
  private def newService[T](network:  Network[Envelope[DataItemAction[T]]])(
      implicit encDec: NioEncDec[T],
      itemEncDec: NioEncDec[DataItem[T]],
      actionEncDec: NioEncDec[DeleteSignatureWrapper[T]],
      actionEnvelopeEncDec: NioEncDec[Envelope[DataItemAction[T]]],
      canValidate: CanValidate[DataItem[T]]) =
    new DataItemService[T](table, network)
  private implicit val dataItemSerializable = mock[NioEncDec[String]]
  private implicit val actionSerializable = mock[NioEncDec[DeleteSignatureWrapper[String]]]
  private implicit val enveloperDataItemEncDec = mock[NioEncDec[Envelope[DataItemAction[String]]]]
  private implicit val dataItemEncDec = mock[NioEncDec[DataItem[String]]]
  private implicit val canValidate = new CanValidate[DataItem[String]] {
    override def validate(t: DataItem[String]): Either[ApplicationError, Unit] = Right(())
  }

  private val dataItem = DataItem("id", "foo", Seq(), Seq())

  private val keypair = generateSigningKeyPair()

  private def signature[T](dataItem: DataItem[T])(implicit nioEncDec: NioEncDec[DataItem[T]]) =
    sign(dataItem, keypair.`private`)

  private def enc(implicit itemEncDec: NioEncDec[DataItem[String]]) = itemEncDec.encode(dataItem)

  val envelopeInsert: Envelope[DataItemAction[String]] =
    Envelope(Insert(dataItem), "nothing", Everyone)

  val signature = mock[Signature]
  val envelopeDelete: Envelope[DataItemAction.Delete[String]] =
    Envelope(Delete(dataItem.id, signature), "nothing", Everyone)
  val f = Frame(FrameHeader(NodeId("957e"), NodeId("0b1a"), 5), envelopeInsert)
  private def fenced(implicit ed: NioEncDec[Frame[Envelope[DataItemAction[String]]]]) = ed.encode(f)
  behavior of "DataItemService"

  it should "insert a data item" in {
    val network = mock[Network[Envelope[DataItemAction[String]]]]
    val messageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]
    when(network.messageStream).thenReturn(messageStream)
    when(messageStream.foreach(any())).thenReturn(Future.successful(()))
    val service = newService[String](network)
    service.processAction(envelopeInsert)
    verify(table).insert(envelopeInsert.containerId, dataItem)
  }

  it should "delete a data item" in {
    val network = mock[Network[Envelope[DataItemAction[String]]]]
    val messageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]
    when(network.messageStream).thenReturn(messageStream)
    when(messageStream.foreach(any())).thenReturn(Future.successful(()))
    val service = newService[String](network)
    service.processAction(envelopeDelete)
    verify(table).delete[String](envelopeDelete.containerId, envelopeDelete.content.itemId, envelopeDelete.content.deleteSignature)
  }
}
