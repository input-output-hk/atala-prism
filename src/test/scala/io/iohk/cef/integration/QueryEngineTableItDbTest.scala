package io.iohk.cef.integration

import java.nio.file.Files
import java.util.UUID

import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto._
import io.iohk.cef.data._
import io.iohk.cef.data.query.{DataItemQueryEngine, DataItemQueryRequest, DataItemQueryResponse, Field}
import io.iohk.cef.data.storage.mv.MVTableStorage
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.network.{Envelope, MessageStream, NodeId}
import io.iohk.cef.test.DummyNoMessageNetwork
import io.iohk.cef.network.Everyone
import io.iohk.cef.utils.NonEmptyList
import monix.execution.schedulers.TestScheduler
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}

import scala.concurrent.Future

class QueryEngineTableItDbTest extends FlatSpec with MustMatchers with EitherValues {

  behavior of "QueryEngineTableItDbTest"

  it should "query existing data items" in {
    val nodeId = NodeId("1111")
    val tableId = "table"
    val path = Files.createTempFile("", "QueryEngineTableItDbTest")
    val storage = new MVTableStorage[String](tableId, path)
    val realTable = new Table[String](tableId, storage)
    implicit val scheduler = TestScheduler()
    val requestNetwork = new DummyNoMessageNetwork[Envelope[DataItemQueryRequest]]
    val responseNetwork = new DummyNoMessageNetwork[Envelope[DataItemQueryResponse[String]]]
    val fakeDataItemNetwork = new DummyNoMessageNetwork[Envelope[DataItemAction[String]]]

    val realEngine =
      new DataItemQueryEngine(nodeId, realTable, requestNetwork, responseNetwork, () => UUID.randomUUID().toString)

    implicit val canValidate: CanValidate[DataItem[String]] = _ => Right(())

    val service = new DataItemService(realTable, fakeDataItemNetwork, realEngine)

    //Given -- there are data items
    val keys = generateSigningKeyPair()

    val data1 = "value1"
    val data2 = "value2"

    val labeledItem1 = LabeledItem.Create(data1)
    val labeledItem2 = LabeledItem.Create(data2)

    val owner1 = Owner(keys.public, sign(labeledItem1, keys.`private`))
    val owner2 = Owner(keys.public, sign(labeledItem2, keys.`private`))

    val witness1 = Witness(owner1.key, sign(data1, keys.`private`))
    val di1 = DataItem("insert1", data1, Seq(witness1), NonEmptyList(owner1))
    val di2 = DataItem("insert2", data2, Seq(), NonEmptyList(owner2))

    val insert1 = Envelope(DataItemAction.InsertAction(di1), tableId, Everyone)
    val insert2 = Envelope(DataItemAction.InsertAction(di2), tableId, Everyone)

    service.processAction(insert1) mustBe Right(DataItemServiceResponse.DIUnit)
    service.processAction(insert2) mustBe Right(DataItemServiceResponse.DIUnit)

    //When -- user queries for id
    val query1 = Field(0) #== "insert1"
    val query2 = Field(0) #== "insert2"
    val query3 = Field(0) #== "insert3"

    def foldStream(
        stream: MessageStream[Either[ApplicationError, Seq[DataItem[String]]]]
    ): Either[ApplicationError, Seq[DataItem[String]]] = {
      stream.foreach(println(_))
      val fold: Future[Either[ApplicationError, Seq[DataItem[String]]]] =
        stream.fold[Either[ApplicationError, Seq[DataItem[String]]]](Right(Seq()))(
          (c, s) =>
            for {
              current <- c
              state <- s
            } yield current ++ state
        )
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
