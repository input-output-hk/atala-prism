package io.iohk.cef.data

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.core.{Envelope, Everyone}
import io.iohk.cef.crypto.{Signature, _}
import io.iohk.cef.data.DataItemAction.Insert
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{NetworkFixture, NodeId}
import io.iohk.cef.network.discovery.NetworkDiscovery
import io.iohk.cef.network.transport.tcp.NetUtils._
import io.iohk.cef.network.transport.{Frame, FrameHeader, Transports}
import org.mockito.Mockito.verify
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._

class DataItemServiceSpec extends FlatSpec with NetworkFixture {

  private val table = mock[Table]
  private def newService[T](transports: Transports, networkDiscovery: NetworkDiscovery)(
      implicit encDec: NioEncDec[T],
      itemEncDec: NioEncDec[DataItem[T]],
      actionEncDec: NioEncDec[DataItemAction[T]],
      actionEnvelopeEncDec: NioEncDec[Envelope[DataItemAction[T]]],
      canValidate: CanValidate[DataItem[T]]) =
    new DataItemService[T](table, transports, networkDiscovery)
  private implicit val dataItemSerializable = mock[NioEncDec[String]]
  private implicit val actionSerializable = mock[NioEncDec[DataItemAction[String]]]
  private implicit val canValidate = new CanValidate[DataItem[String]] {
    override def validate(t: DataItem[String]): Either[ApplicationError, Unit] = Right(())
  }

  private val dataItem = DataItem("id", "foo", Seq(), Seq())

  private val keypair = generateSigningKeyPair()

//  private implicit val enc: NioEncoder[StringDataItem] = NioEncoder[StringDataItem]
//  private implicit val dec: NioDecoder[StringDataItem] = NioDecoder[StringDataItem]

  private def signature[T](dataItem: DataItem[T])(implicit nioEncDec: NioEncDec[DataItem[T]]) =
    sign(dataItem, keypair.`private`)
//  private implicit val codec: NioEncDec[DataItem[String]] = new NioEncDec[DataItem[String]] {
//    override def decode(u: ByteBuffer) = Some(dataItem)
//    override def encode(t: DataItem[String]) = ByteBuffer.
//  }

  private def enc(implicit itemEncDec: NioEncDec[DataItem[String]]) = itemEncDec.encode(dataItem)
// : Frame[Envelope[DataItemAction[String]]]
  val envelopeAction: Envelope[DataItemAction[String]] =
    Envelope(Insert(DataItem("foo", "", List(), List())), "nothing", Everyone)
  val f = Frame(FrameHeader(NodeId("957e"), NodeId("0b1a"), 5), envelopeAction)
  private def fenced(implicit ed: NioEncDec[Frame[Envelope[DataItemAction[String]]]]) = ed.encode(f)
  behavior of "DataItemService"

  val bootstrap = randomBaseNetwork(None)
  it should "insert a data item" in networks(bootstrap, randomBaseNetwork(Some(bootstrap))) { networks =>
    {
      val dataItem: DataItem[String] = mock[DataItem[String]]
      val service = newService[String](networks.head.transports, networks.head.networkDiscovery)
      service.insert(envelopeAction.map(_.dataItem))
      verify(table).insert(envelopeAction.containerId, dataItem)
    }
  }

  it should "delete a data item" in networks(bootstrap, randomBaseNetwork(Some(bootstrap))) { networks =>
    {
      val dataItem: DataItem[String] = mock[DataItem[String]]
      val signature = mock[Signature]
      val service = newService[String](networks.head.transports, networks.head.networkDiscovery)
      service.delete(envelopeAction.map(_.dataItem), signature)
      verify(table).delete(envelopeAction.containerId, envelopeAction.content.dataItem, signature)
    }
  }
}
