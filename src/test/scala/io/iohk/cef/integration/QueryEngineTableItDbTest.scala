package io.iohk.cef.integration

import java.nio.file.Files
import java.util.UUID

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto.generateSigningKeyPair
import io.iohk.cef.data._
import io.iohk.cef.data.query.{Field, QueryEngine, QueryRequest, QueryResponse}
import io.iohk.cef.data.storage.mv.MVTableStorage
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, NodeId}
import io.iohk.cef.test.DummyNoMessageNetwork
import io.iohk.cef.transactionservice.{Envelope, Everyone}
import monix.execution.schedulers.TestScheduler
import org.scalactic.Every
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}

import scala.concurrent.Future

class QueryEngineTableItDbTest extends FlatSpec with MustMatchers with EitherValues {

  private val defaultOwner = Owner(generateSigningKeyPair().public)
  behavior of "QueryEngineTableItDbTest"

  it should "query existing data items" in {
    val nodeId = NodeId("1111")
    val tableId = "table"
    val path = Files.createTempFile("", "QueryEngineTableItDbTest")
    val storage = new MVTableStorage[String](tableId, path)
    val realTable = new Table[String](tableId, storage)
    implicit val scheduler = TestScheduler()
    val requestNetwork = new DummyNoMessageNetwork[Envelope[QueryRequest]]
    val responseNetwork = new DummyNoMessageNetwork[Envelope[QueryResponse[String]]]
    val fakeDataItemNetwork = new DummyNoMessageNetwork[Envelope[DataItemAction[String]]]

    val realEngine =
      new QueryEngine(nodeId, realTable, requestNetwork, responseNetwork, () => UUID.randomUUID().toString)

    implicit val canValidate: CanValidate[DataItem[String]] = _ => Right(())

    val service = new DataItemService(realTable, fakeDataItemNetwork, realEngine)

    //Given -- there are data items
    val di1 = DataItem("insert1", "value1", Seq(), Every(defaultOwner))
    val di2 = DataItem("insert2", "value2", Seq(), Every(defaultOwner))
    val insert1 = Envelope(DataItemAction.InsertAction(di1), tableId, Everyone)
    val insert2 = Envelope(DataItemAction.InsertAction(di2), tableId, Everyone)

    service.processAction(insert1) mustBe Right(DataItemServiceResponse.DIUnit)
    service.processAction(insert2) mustBe Right(DataItemServiceResponse.DIUnit)

    //When -- user queries for id
    val query1 = Field(0) #== "insert1"
    val query2 = Field(0) #== "insert2"
    val query3 = Field(0) #== "insert3"

    def foldStream(stream: MessageStream[Either[ApplicationError, Seq[DataItem[String]]]])
      : Either[ApplicationError, Seq[DataItem[String]]] = {
      stream.foreach(println(_))
      val fold: Future[Either[ApplicationError, Seq[DataItem[String]]]] =
        stream.fold[Either[ApplicationError, Seq[DataItem[String]]]](Right(Seq()))((c, s) =>
          for {
            current <- c
            state <- s
          } yield current ++ state)
      fold.futureValue
    }

    scheduler.tick()

    val queryResult1 = foldStream(service.processQuery(Envelope(query1, tableId, Everyone)))
    val queryResult2 = foldStream(service.processQuery(Envelope(query2, tableId, Everyone)))
    val queryResult3 = foldStream(service.processQuery(Envelope(query3, tableId, Everyone)))

    //Then -- data items should've been returned

    queryResult1 mustBe Right(Seq(di1))
    queryResult2 mustBe Right(Seq(di2))
    queryResult3 mustBe Right(Seq())
  }
}
