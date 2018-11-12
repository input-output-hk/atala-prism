package io.iohk.cef.integration
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.core.{Envelope, Everyone}
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import io.iohk.cef.data.storage.scalike.TableStorageImpl
import io.iohk.cef.data.storage.scalike.dao.TableStorageDao
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback

import scala.concurrent.Future

trait DataItemServiceTableDbItSpec
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with MockitoSugar
    with EitherValues {

  behavior of "DataItemServiceTableIt"

  val mockedNetwork = mock[Network[Envelope[DataItemAction[String]]]]
  val mockMessageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]

  when(mockedNetwork.messageStream).thenReturn(mockMessageStream)
  when(mockMessageStream.foreach(any())).thenReturn(Future.successful(()))

  it should "write in the database new data items" in { implicit s =>
    val tableId: TableId = "TableId"
    val ownerKeyPair = generateSigningKeyPair()
    val ownerKeyPair2 = generateSigningKeyPair()
    val firstDataItem = DataItem("id1", "data1", Seq(), Seq(Owner(ownerKeyPair.public)))
    val dataItems = Seq(firstDataItem, DataItem("id2", "data2", Seq(), Seq(Owner(ownerKeyPair2.public))))

    val dao = new TableStorageDao

    val itemsBefore = dao.selectAll[String](tableId, dataItems.map(_.id))
    itemsBefore mustBe Right(Seq())

    val tableStorage = new TableStorageImpl(dao)
    val table = new Table(tableStorage)

    implicit val canValidate = new CanValidate[DataItem[String]] {
      override def validate(t: DataItem[String]): Either[ApplicationError, Unit] = Right(())
    }

    val service = new DataItemService(table, mockedNetwork)

    val envelopes = dataItems.map(di => Envelope(DataItemAction.Insert(di), tableId, Everyone))

    val results = envelopes.map(service.processAction)
    envelopes.foreach(e => verify(mockedNetwork, times(1)).disseminateMessage(e))
    results.foreach(result => result mustBe Right(()))

    val itemsAfter = dao.selectAll[String](tableId, dataItems.map(_.id))
    itemsAfter.map(_.toSet) mustBe Right(envelopes.map(_.content.dataItem).toSet)

    val deleteSignature = DeleteSignatureWrapper(firstDataItem)
    val deleteAction: DataItemAction[String] =
      DataItemAction.Delete(firstDataItem.id, sign(deleteSignature, ownerKeyPair.`private`))
    val deleteResult = service.processAction(Envelope(deleteAction, tableId, Everyone))
    deleteResult mustBe Right(())

    val itemsAfterDelete = dao.selectAll[String](tableId, dataItems.map(_.id))
    itemsAfterDelete.map(_.toSet) mustBe Right(envelopes.tail.map(_.content.dataItem).toSet)

  }
}
