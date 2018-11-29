package io.iohk.cef.integration

import java.nio.file.{Files, Path}

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data.DataItemAction.InsertAction
import io.iohk.cef.data._
import io.iohk.cef.data.query.Query.NoPredicateQuery
import io.iohk.cef.data.query.QueryEngine
import io.iohk.cef.data.storage.mv.MVTableStorage
import io.iohk.cef.network.{MessageStream, Network}
import io.iohk.cef.transactionservice.{Envelope, Everyone}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._
import org.scalatest.mockito.MockitoSugar._

import scala.concurrent.Future

class DataItemServiceTableItSpec extends FlatSpec {

  behavior of "DataItemServiceTableIt"

  private val mockedNetwork = mock[Network[Envelope[DataItemAction[String]]]]
  private val mockMessageStream = mock[MessageStream[Envelope[DataItemAction[String]]]]

  when(mockedNetwork.messageStream).thenReturn(mockMessageStream)
  when(mockMessageStream.foreach(any())).thenReturn(Future.successful(()))

  private val testTableId = "TableId"
  private val ownerKeyPair = generateSigningKeyPair()
  private val ownerKeyPair2 = generateSigningKeyPair()
  private val firstDataItem = DataItem("id1", "data1", Seq(), Seq(Owner(ownerKeyPair.public)))
  private val secondDataItem = DataItem("id2", "data2", Seq(), Seq(Owner(ownerKeyPair2.public)))
  private val dataItems: Seq[DataItem[DataItemId]] = Seq(firstDataItem, secondDataItem)

  private val envelopes: Seq[Envelope[DataItemAction[DataItemId]]] =
    dataItems.map(di => Envelope(InsertAction(di): DataItemAction[DataItemId], testTableId, Everyone))

  implicit val canValidate: CanValidate[DataItem[String]] = _ => Right(())

  it should "insert and delete items in the database" in testStorage { storage =>
    val table = new Table[String](testTableId, storage)
    table.select(NoPredicateQuery) mustBe Right(Seq())

    val service = new DataItemService[String](table, mockedNetwork, mock[QueryEngine[String]])

    val results = envelopes.map(service.processAction)
    envelopes.foreach(e => verify(mockedNetwork, times(1)).disseminateMessage(e))
    results.foreach(result => result mustBe Right(()))

    val itemsAfter = table.select(NoPredicateQuery)
    itemsAfter.map(_.toSet) mustBe Right(envelopes.map {
      case Envelope(InsertAction(di), _, _) => di
      case other => fail(s"Unexpected action received. Expected Insert but got ${other}")
    }.toSet)

    val deleteSignature = DeleteSignatureWrapper(firstDataItem)
    val deleteAction: DataItemAction[String] =
      DataItemAction.DeleteAction(firstDataItem.id, sign(deleteSignature, ownerKeyPair.`private`))
    val deleteResult = service.processAction(Envelope(deleteAction, testTableId, Everyone))
    deleteResult mustBe Right(())

    val itemsAfterDelete = table.select(NoPredicateQuery)
    itemsAfterDelete.map(_.toSet) mustBe Right(envelopes.tail.map {
      case Envelope(InsertAction(di), _, _) => di
      case other => fail(s"Unexpected action received. Expected Insert but got ${other}")
    }.toSet)
  }

  private def testStorage(testCode: MVTableStorage[String] => Any): Unit = {
    val tempFile: Path = Files.createTempFile("", "")
    val storage = new MVTableStorage[String](testTableId, tempFile)
    try {
      testCode(storage)
    } finally {
      Files.delete(tempFile)
    }
  }
}
