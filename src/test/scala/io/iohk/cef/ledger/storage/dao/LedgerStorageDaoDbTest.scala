package io.iohk.cef.ledger.storage.dao

import java.time.{Clock, Instant}

import akka.util.ByteString
import io.iohk.cef.builder.RSAKeyGenerator
import io.iohk.cef.crypto.low.DigitalSignature
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.storage.scalike.LedgerTable
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import org.scalatest.{MustMatchers, OptionValues, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback

trait LedgerStorageDaoDbTest extends fixture.FlatSpec
  with AutoRollback
  with RSAKeyGenerator
  with OptionValues
  with MustMatchers {

  behavior of "LedgerStorageImpl"

  val dummySignature = new DigitalSignature(ByteString.empty)

  it should "update the ledger" in { implicit session =>
    val pair1 = generateKeyPair
    val pair2 = generateKeyPair
    val pair3 = generateKeyPair

    val header = IdentityBlockHeader(ByteString("hash"), Instant.now, 1)
    val txList = List[IdentityTransaction](
      Claim("one", pair1._1, dummySignature),
      Link("two", pair2._1, IdentityTransaction.sign("two", pair2._1, pair2._2)))
    val block = Block(header, txList)
    val storage = new LedgerStorageDao(Clock.systemUTC())
    storage.push(1, block)(IdentityBlockSerializer.serializable, session)

    val lt = LedgerTable.syntax("lt")
    val blockDataInDb = sql"""select ${lt.result.*} from ${LedgerTable as lt}"""
      .map(rs => LedgerTable(lt.resultName)(rs)).single().apply()

    val blockEntry = blockDataInDb.value
    val dbBlock = IdentityBlockSerializer.serializable.deserialize(blockEntry.data)
    dbBlock.header mustBe header
    dbBlock.transactions mustBe txList
  }

}
