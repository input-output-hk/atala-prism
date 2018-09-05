package io.iohk.cef.ledger

import io.iohk.cef.ledger.storage.{Ledger, LedgerStateStorage, LedgerStorage}
import io.iohk.cef.utils.ForExpressionsEnabler
import scalikejdbc._

import scala.util.Try

trait LedgerFixture {

  def createLedger[S](stateStorage: LedgerStateStorage[S], storage: LedgerStorage)(
      implicit dBSession: DBSession): Ledger[Try, S] = {
    implicit val forExpEnabler = ForExpressionsEnabler.tryEnabler
    Ledger(1, storage, stateStorage)
  }
}
