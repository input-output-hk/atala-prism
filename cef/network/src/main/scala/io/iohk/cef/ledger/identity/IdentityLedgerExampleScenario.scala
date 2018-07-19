package io.iohk.cef.ledger.identity

import java.time.Instant

import akka.util.ByteString
import io.iohk.cef.ledger.Block
import io.iohk.cef.ledger.identity.storage.LedgerStateStorageImpl
import io.iohk.cef.ledger.storage.Ledger
import io.iohk.cef.ledger.storage.scalike.LedgerStorageImpl
import io.iohk.cef.utils.ForExpressionsEnabler._

import scala.concurrent.Await
import scala.concurrent.duration._

object IdentityLedgerExampleScenario extends App {

  val ledgerStateStorage = new LedgerStateStorageImpl()

  val ledgerStorage = new LedgerStorageImpl()

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
