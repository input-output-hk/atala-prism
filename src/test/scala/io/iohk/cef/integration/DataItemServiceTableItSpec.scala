package io.iohk.cef.integration

import java.nio.file.{Files, Path}

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import io.iohk.cef.data.query.Query.NoPredicateQuery
import io.iohk.cef.data.storage.mv.MVTableStorage
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, Network}
import io.iohk.cef.transactionservice.{Envelope, Everyone}

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{when, verify, times}

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
  private val dataItems = Seq(firstDataItem, secondDataItem)

  val envelopes = dataItems.map(di => Envelope(DataItemAction.Insert(di), testTableId, Everyone))

  implicit val canValidate = new CanValidate[DataItem[String]] {
    override def validate(t: DataItem[String]): Either[ApplicationError, Unit] = Right(())
  }

  it should "insert and delete items in the database" in testStorage { storage =>
    val table = new Table[String](testTableId, storage)
    table.select(NoPredicateQuery) mustBe Right(Seq())

    val service: DataItemService[String] = new DataItemService(table, mockedNetwork)

    val results = envelopes.map(service.processAction)
    envelopes.foreach(e => verify(mockedNetwork, times(1)).disseminateMessage(e))
    results.foreach(result => result mustBe Right(()))

    table.select(NoPredicateQuery).map(_.toSet) mustBe Right(envelopes.map(_.content.dataItem).toSet)

    val deleteSignature = DeleteSignatureWrapper(firstDataItem)
    val deleteAction: DataItemAction[String] =
      DataItemAction.Delete(firstDataItem.id, sign(deleteSignature, ownerKeyPair.`private`))
    val deleteResult = service.processAction(Envelope(deleteAction, testTableId, Everyone))
    deleteResult mustBe Right(())

    table.select(NoPredicateQuery).map(_.toSet) mustBe Right(envelopes.tail.map(_.content.dataItem).toSet)
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
