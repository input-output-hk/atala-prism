package io.iohk.cef.integration
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.transactionservice.{Envelope, Everyone}
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import io.iohk.cef.data.query.Query.NoPredicateQuery
import io.iohk.cef.data.storage.scalike.dao.TableStorageDao
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc.DBSession
import scalikejdbc.scalatest.AutoRollback

import scala.concurrent.Future

trait DataItemServiceTableDbItSpec
    extends fixture.FlatSpec
    with AutoRollback
    with MustMatchers
    with EitherValues
    with MockitoSugar {

  behavior of "DataItemServiceTableIt"

  val mockedNetwork = mock[Network[Envelope[DataItemAction[String]]]]
  val mockMessageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]

  when(mockedNetwork.messageStream).thenReturn(mockMessageStream)
  when(mockMessageStream.foreach(any())).thenReturn(Future.successful(()))

  val testTableId = "TableId"
  val ownerKeyPair = generateSigningKeyPair()
  val ownerKeyPair2 = generateSigningKeyPair()
  val firstDataItem = DataItem("id1", "data1", Seq(), Seq(Owner(ownerKeyPair.public)))
  val secondDataItem = DataItem("id2", "data2", Seq(), Seq(Owner(ownerKeyPair2.public)))
  val dataItems = Seq(firstDataItem, secondDataItem)

  val envelopes = dataItems.map(di => Envelope(DataItemAction.Insert(di), testTableId, Everyone))

  implicit val canValidate = new CanValidate[DataItem[String]] {
    override def validate(t: DataItem[String]): Either[ApplicationError, Unit] = Right(())
  }

  def getItems(dao: TableStorageDao, tableId: TableId)(implicit session: DBSession) =
    dao.selectWithQuery[String](tableId, NoPredicateQuery)

  it should "insert and delete items in the database" in { implicit s =>
    new RealTableFixture {
      override val tableId: TableId = testTableId
      val table = new Table[String](tableId, tableStorage)
      val itemsBefore = getItems(dao, tableId)
      itemsBefore mustBe Right(Seq())

      val service: DataItemService[String] = new DataItemService(table, mockedNetwork)

      val results = envelopes.map(service.processAction)
      envelopes.foreach(e => verify(mockedNetwork, times(1)).disseminateMessage(e))
      results.foreach(result => result mustBe Right(()))

      val itemsAfter = getItems(dao, tableId)
      itemsAfter.map(_.toSet) mustBe Right(envelopes.map(_.content.dataItem).toSet)

      val deleteSignature = DeleteSignatureWrapper(firstDataItem)
      val deleteAction: DataItemAction[String] =
        DataItemAction.Delete(firstDataItem.id, sign(deleteSignature, ownerKeyPair.`private`))
      val deleteResult = service.processAction(Envelope(deleteAction, tableId, Everyone))
      deleteResult mustBe Right(())

      val itemsAfterDelete = getItems(dao, tableId)
      itemsAfterDelete.map(_.toSet) mustBe Right(envelopes.tail.map(_.content.dataItem).toSet)
    }
  }
}
