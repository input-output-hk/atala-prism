package io.iohk.cef.integration
import java.util.UUID

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.data._
import io.iohk.cef.data.query.{Field, QueryEngine, QueryRequest, QueryResponse}
import io.iohk.cef.data.storage.scalike.TableStorageImpl
import io.iohk.cef.data.storage.scalike.dao.TableStorageDao
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{MessageStream, NodeId}
import io.iohk.cef.test.DummyNoMessageNetwork
import io.iohk.cef.transactionservice.{Envelope, Everyone}
import monix.execution.schedulers.TestScheduler
import org.scalatest.{EitherValues, MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import org.scalatest.concurrent.ScalaFutures._

trait QueryEngineTableItDbTest extends fixture.FlatSpec with AutoRollback with MustMatchers with EitherValues {

  behavior of "QueryEngineTableItDbTest"

  it should "query existing data items" in { implicit s =>
    val nodeId = NodeId("1111")
    val tableId = "table"
    val dao = new TableStorageDao
    val storage = new TableStorageImpl(dao)
    val realTable = new Table(storage)
    implicit val scheduler = TestScheduler()
    val requestNetwork = new DummyNoMessageNetwork[Envelope[QueryRequest]]
    val responseNetwork = new DummyNoMessageNetwork[Envelope[QueryResponse[String]]]
    val fakeDataItemNetwork = new DummyNoMessageNetwork[Envelope[DataItemAction[String]]]

    val realEngine =
      new QueryEngine(nodeId, realTable, requestNetwork, responseNetwork, () => UUID.randomUUID().toString)

    implicit val canValidate: CanValidate[DataItem[String]] = _ => Right(())

    val service = new DataItemService(realTable, fakeDataItemNetwork, realEngine)

    //Given -- there are data items
    val di1 = DataItem("insert1", "value1", Seq(), Seq())
    val di2 = DataItem("insert2", "value2", Seq(), Seq())
    val insert1 = Envelope(DataItemAction.insert(di1), tableId, Everyone)
    val insert2 = Envelope(DataItemAction.insert(di2), tableId, Everyone)

    service.processAction(insert1) mustBe Right(())
    service.processAction(insert2) mustBe Right(())

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
