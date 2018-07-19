package io.iohk.cef.ledger.identity.storage

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.db.AutoRollbackSpec
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.storage.scalike.{LedgerStorageImpl, LedgerTable}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class LedgerStorageImplSpec extends fixture.FlatSpec
  with AutoRollbackSpec
  with MustMatchers
  with MockFactory {

  def createStorage(session: DBSession) = {
    new LedgerStorageImpl {
      override def inFutureTx[T](f: DBSession => Future[T]): Future[T] = {
        val result = f(session)
        Await.ready(result, 15 seconds)
        result
      }
    }
  }

  behavior of "LedgerStorageImpl"

  it should "update the ledger" in { implicit session =>
    val header = IdentityBlockHeader(ByteString("hash"), Instant.now, 1)
    val txList = List[IdentityTransaction](Claim("one", ByteString("one")), Link("two", ByteString("two")))
    val block = Block(header, txList)
    val storage = createStorage(session)
    Await.ready(storage.push(1, block)(IdentityBlockSerializer.serializable), 30 seconds)
    val lt = LedgerTable.syntax("lt")
    val blockDataInDb = sql"""select ${lt.result.*} from ${LedgerTable as lt}"""
      .map(rs => LedgerTable(lt.resultName)(rs)).single().apply()
    blockDataInDb.isDefined mustBe true
    val blockEntry = blockDataInDb.get
    val dbBlock = IdentityBlockSerializer.serializable.deserialize(blockEntry.data)
    dbBlock.header mustBe header
    dbBlock.transactions mustBe txList
  }

}
