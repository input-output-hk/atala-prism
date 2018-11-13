package io.iohk.cef.data

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.core.{Envelope, Everyone}
import io.iohk.cef.crypto.Signature
import io.iohk.cef.data.DataItemAction.{Delete, Insert}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._

import scala.concurrent.Future

class DataItemServiceSpec extends FlatSpec {

  private val table = mock[Table]
  private implicit val dataItemSerializable = mock[NioEncDec[String]]
  private implicit val actionSerializable = mock[NioEncDec[DataItemAction[String]]]
  private implicit val enveloperDataItemEncDec = mock[NioEncDec[Envelope[DataItemAction[String]]]]
  private implicit val dataItemEncDec = mock[NioEncDec[DataItem[String]]]
  private implicit val deleteSigWrapperCodec = mock[NioEncDec[DeleteSignatureWrapper[String]]]
  private implicit val canValidate = new CanValidate[DataItem[String]] {
    override def validate(t: DataItem[String]): Either[ApplicationError, Unit] = Right(())
  }

  private val dataItem: DataItem[String] = DataItem("id", "foo", Seq(), Seq())
  private val containerId = "container-id"

  behavior of "DataItemService"

  it should "insert a data item" in {
    val network = mock[Network[Envelope[DataItemAction[String]]]]
    val messageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]
    when(network.messageStream).thenReturn(messageStream)
    when(messageStream.foreach(any())).thenReturn(Future.successful(()))
    val service: DataItemService[String] = new DataItemService(table, network)

    service.processAction(Envelope(Insert(dataItem), containerId, Everyone))

    verify(table).insert(containerId, dataItem)
  }

  it should "delete a data item" in {
    val signature = mock[Signature]
    val network = mock[Network[Envelope[DataItemAction[String]]]]
    val messageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]
    when(network.messageStream).thenReturn(messageStream)
    when(messageStream.foreach(any())).thenReturn(Future.successful(()))

    val service = new DataItemService(table, network)

    service.processAction(Envelope(Delete(dataItem.id, signature), containerId, Everyone))

    verify(table).delete[String](containerId, dataItem.id, signature)
  }
}
