package io.iohk.cef.ledger.storage.dao

import java.time.{Clock, Instant}

import io.iohk.cef.builder.SigningKeyPairs
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.{Block, BlockHeader}
import io.iohk.cef.ledger.identity._
import io.iohk.cef.ledger.storage.scalike.LedgerTable
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import org.scalatest.{MustMatchers, OptionValues, fixture}
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._

trait LedgerStorageDaoDbTest
    extends fixture.FlatSpec
    with AutoRollback
    with SigningKeyPairs
    with OptionValues
    with MustMatchers {

  behavior of "LedgerStorageImpl"

  it should "update the ledger" in { implicit session =>
    implicit val codec = NioEncDec[Block[IdentityData, IdentityTransaction]]
    val header = BlockHeader(Instant.now)
    val txList = List[IdentityTransaction](
      Claim("one", alice.public, uselessSignature),
      Link("two", bob.public, IdentityTransaction.sign("two", IdentityTransactionType.Link, bob.public, bob.`private`))
    )
    val block = Block[IdentityData, IdentityTransaction](header, txList)
    val storage = new LedgerStorageDao(Clock.systemUTC())
    storage.push("1", block)

    val lt = LedgerTable.syntax("lt")
    val blockDataInDb = sql"""select ${lt.result.*} from ${LedgerTable as lt}"""
      .map(rs => LedgerTable(lt.resultName)(rs))
      .single()
      .apply()

    val blockEntry = blockDataInDb.value
    val dbBlock = codec.decode(blockEntry.data.toByteBuffer)
    dbBlock.isDefined mustBe true
    dbBlock.get.header mustBe header
    dbBlock.get.transactions mustBe txList
  }

}
