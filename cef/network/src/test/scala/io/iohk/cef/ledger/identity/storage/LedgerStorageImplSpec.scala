package io.iohk.cef.ledger.identity.storage

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.db.AutoRollbackSpec
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.identity.storage.db.{IdentityLedgerBlockTable, IdentityLedgerTransactionTable}
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
    val header = IdentityBlockHeader(ByteString("hash"), Instant.now)
    val txList = List(Claim("one", ByteString("one")), Link("two", ByteString("two")))
    val block = Block[IdentityLedgerState, String, IdentityBlockHeader, IdentityTransaction](header, txList)
    val storage = createStorage(session)
    storage.push(block)
    val bt = IdentityLedgerBlockTable.syntax("bt")
    val tt = IdentityLedgerTransactionTable.syntax("tt")
    val blockDataInDb = sql"""select ${bt.result.*} from ${IdentityLedgerBlockTable as bt}"""
      .map(rs => (rs.long(bt.resultName.id), ByteString(rs.bytes(bt.resultName.hash)), rs.timestamp(bt.resultName.created).toInstant)).single().apply()
    blockDataInDb.isDefined mustBe true
    val (blockId, hash, created) = blockDataInDb.get
    hash mustBe header.hash
    created mustBe header.created
    val txDataInDb =
      sql"""select ${tt.result.*} from ${IdentityLedgerTransactionTable as tt}"""
      .map(rs => (IdentityLedgerTransactionTable(tt.resultName)(rs), rs.long(tt.resultName.blockId))).list().apply
    txDataInDb.map(_._1).toSet mustBe txList.toSet
    txDataInDb.map(_._2).foreach(_ mustBe blockId)
  }

}
