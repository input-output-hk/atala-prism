package io.iohk.cef.data

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.core.{Envelope, Everyone}
import io.iohk.cef.crypto.{Signature, _}
import io.iohk.cef.data.DataItemAction.Insert
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
      actionEncDec: NioEncDec[DataItemAction[T]],
      actionEnvelopeEncDec: NioEncDec[Envelope[DataItemAction[T]]],
      canValidate: CanValidate[DataItem[T]]) =
    new DataItemService[T](table, network)
  private implicit val dataItemSerializable = mock[NioEncDec[String]]
  private implicit val actionSerializable = mock[NioEncDec[DataItemAction[String]]]
  private implicit val canValidate = new CanValidate[DataItem[String]] {
    override def validate(t: DataItem[String]): Either[ApplicationError, Unit] = Right(())
  }

  private val dataItem = DataItem("id", "foo", Seq(), Seq())

  private val keypair = generateSigningKeyPair()

  private def signature[T](dataItem: DataItem[T])(implicit nioEncDec: NioEncDec[DataItem[T]]) =
    sign(dataItem, keypair.`private`)

  private def enc(implicit itemEncDec: NioEncDec[DataItem[String]]) = itemEncDec.encode(dataItem)
  val envelopeAction: Envelope[DataItemAction[String]] =
    Envelope(Insert(DataItem("foo", "", List(), List())), "nothing", Everyone)
  val f = Frame(FrameHeader(NodeId("957e"), NodeId("0b1a"), 5), envelopeAction)
  private def fenced(implicit ed: NioEncDec[Frame[Envelope[DataItemAction[String]]]]) = ed.encode(f)
  behavior of "DataItemService"

  it should "insert a data item" in {
    val network = mock[Network[Envelope[DataItemAction[String]]]]
    val messageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]
    when(network.messageStream).thenReturn(messageStream)
    when(messageStream.foreach(any())).thenReturn(Future.successful(()))
    val service = newService[String](network)
    service.insert(envelopeAction.map(_.dataItem))
    verify(table).insert(envelopeAction.containerId, envelopeAction.content.dataItem)
  }

  it should "delete a data item" in {
    val signature = mock[Signature]
    val network = mock[Network[Envelope[DataItemAction[String]]]]
    val messageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]
    when(network.messageStream).thenReturn(messageStream)
    when(messageStream.foreach(any())).thenReturn(Future.successful(()))
    val service = newService[String](network)
    service.delete(envelopeAction.map(_.dataItem), signature)
    verify(table).delete(envelopeAction.containerId, envelopeAction.content.dataItem, signature)
  }
}
