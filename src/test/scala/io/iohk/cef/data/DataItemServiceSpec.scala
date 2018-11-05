package io.iohk.cef.data

import io.iohk.cef.core.{Envelope, Everyone}
import io.iohk.cef.crypto._
import io.iohk.cef.codecs.nio._
import io.iohk.cef.data.DataItemAction.Insert
import io.iohk.cef.network.NodeId
import io.iohk.cef.network.transport.tcp.NetUtils._
import io.iohk.cef.network.transport.{Frame, FrameHeader}
import org.mockito.Mockito.verify
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._

class DataItemServiceSpec extends FlatSpec {

  case class StringDataItem(data: String, id: String = "", witnesses: Seq[Witness] = Seq(), owners: Seq[Owner] = Seq())
      extends DataItem[String] {
    override def apply(): Either[DataItemError, Unit] = Right(())
  }

  private val dataItem = StringDataItem("foo")

  private val keypair = generateSigningKeyPair()

//  private implicit val enc: NioEncoder[StringDataItem] = NioEncoder[StringDataItem]
//  private implicit val dec: NioDecoder[StringDataItem] = NioDecoder[StringDataItem]

  private val signature = sign(dataItem, keypair.`private`)
//  private implicit val codec: NioEncDec[DataItem[String]] = new NioEncDec[DataItem[String]] {
//    override def decode(u: ByteBuffer) = Some(dataItem)
//    override def encode(t: DataItem[String]) = ByteBuffer.
//  }

  val enc = io.iohk.cef.codecs.nio.NioEncoder[StringDataItem].encode(dataItem)
// : Frame[Envelope[DataItemAction[String]]]
  val envelope: Envelope[DataItemAction[String]] =
    Envelope(Insert(StringDataItem("foo", "", List(), List())), "nothing", Everyone)
  val f = Frame(FrameHeader(NodeId("957e"), NodeId("0b1a"), 5), envelope)

  val fEnc = io.iohk.cef.codecs.nio.NioEncoder[Frame[Envelope[DataItemAction[String]]]]
  val fenced = fEnc.encode(f)
  println(fenced)

  println(enc)

  behavior of "DataItemService"

  it should "insert a data item" in forTwoArbitraryNetworkPeers(verifyInsert)

//  it should "delete a data item" in forTwoArbitraryNetworkPeers(verifyDelete)

  private def verifyInsert(node1: NetworkFixture, node2: NetworkFixture) = {
    val node1Table = mock[Table]
    val node2Table = mock[Table]

    val node1DataItemService = new DataItemService[String](node1Table, node1.transports, node1.networkDiscovery)
    val _ = new DataItemService[String](node2Table, node2.transports, node2.networkDiscovery)

    node1DataItemService.insert(Envelope(dataItem, "nothing", Everyone))

    verify(node1Table).insert(dataItem)
    verify(node2Table).insert(dataItem)
  }

  private def verifyDelete(node1: NetworkFixture, node2: NetworkFixture) = {
    val node1Table = mock[Table]
    val node2Table = mock[Table]

    val node1DataItemService = new DataItemService[String](node1Table, node1.transports, node1.networkDiscovery)
    val _ = new DataItemService[String](node2Table, node2.transports, node2.networkDiscovery)

    node1DataItemService.delete(Envelope(dataItem, "nothing", Everyone), signature)

    verify(node1Table).delete(dataItem, signature)
    verify(node2Table).delete(dataItem, signature)
  }
}
