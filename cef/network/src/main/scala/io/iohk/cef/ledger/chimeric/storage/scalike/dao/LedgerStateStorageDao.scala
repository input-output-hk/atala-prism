package io.iohk.cef.ledger.chimeric.storage.scalike.dao

import io.iohk.cef.ledger.LedgerState
import io.iohk.cef.ledger.chimeric.{AddressHolder, ChimericLedgerState, ChimericStateValue}

class LedgerStateStorageDao {

  def slice(keys: Set[String]): LedgerState[ChimericStateValue] = {
    val stateKeys = keys.map(ChimericLedgerState.toStateKey)
    ???
  }

  def update(previousState: LedgerState[ChimericStateValue],
             newState: LedgerState[ChimericStateValue]): Unit = {
    ???
  }

  private def readAddresses(stateKeys: Seq[AddressHolder]) = ???
}
