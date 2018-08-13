package io.iohk.cef.ledger.storage.dao

import java.time.{Clock, Instant}

import akka.util.ByteString
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.storage.scalike.LedgerTable
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import org.scalamock.scalatest.MockFactory
import org.scalatest.{MustMatchers, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback

trait LedgerStorageDaoDbTest extends fixture.FlatSpec
  with AutoRollback
  with MustMatchers
  with MockFactory {

  behavior of "LedgerStorageImpl"

  it should "update the ledger" in { implicit session =>
    val header = IdentityBlockHeader(ByteString("hash"), Instant.now, 1)
    val txList = List[IdentityTransaction](Claim("one", ByteString("one")), Link("two", ByteString("two")))
    val block = Block(header, txList)
    val storage = new LedgerStorageDao(Clock.systemUTC())
    storage.push(1, block)(IdentityBlockSerializer.serializable, session)
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
