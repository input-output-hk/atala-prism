package io.iohk.cef.ledger.identity

import java.time.{Clock, Instant}

import akka.util.ByteString
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.storage.scalike.LedgerStateStorageImpl
import io.iohk.cef.ledger.identity.storage.scalike.dao.LedgerStateStorageDao
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.ledger.storage.scalike.dao.LedgerStorageDao
import io.iohk.cef.utils.ForExpressionsEnabler
import scalikejdbc.config.DBs

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object IdentityLedgerExampleScenario extends App {

  DBs.setupAll()

  val ledgerStateStorageDao = new LedgerStateStorageDao()

  val ledgerStateStorage = new LedgerStateStorageImpl(ledgerStateStorageDao)

  val ledgerStorageDao = new LedgerStorageDao(Clock.systemUTC())

  val ledgerStorage = new LedgerStorageImpl(ledgerStorageDao)

  implicit val forExpEnabler = ForExpressionsEnabler.futureEnabler

  val identityLedger = Ledger(ledgerStorage, ledgerStateStorage)

  val txs = List[IdentityTransaction](
    Claim("carlos11", ByteString("carlos")),
    Link("carlos", ByteString("vargas"))
  )

  val block = Block(IdentityBlockHeader(ByteString("hash1"), Instant.now(), 1), txs)

  import IdentityBlockSerializer._

  val newLedger = identityLedger.apply(1, block).map(future => Await.result(future, 1 hour))

  println(newLedger)
}
