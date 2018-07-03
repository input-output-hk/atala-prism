package io.iohk.cef.ledger.identity2

import io.iohk.cef.ledger.LedgerError

sealed trait IdentityLedgerError extends LedgerError

class IdentityNotClaimedError(cause: Exception) extends IdentityLedgerError

object IdentityNotClaimedError {
  def apply(cause: Exception): IdentityNotClaimedError = new IdentityNotClaimedError(cause)
}

class IdentityTakenError(cause: Exception) extends IdentityLedgerError

object IdentityTakenError {
  def apply(cause: Exception): IdentityTakenError = new IdentityTakenError(cause)
}
