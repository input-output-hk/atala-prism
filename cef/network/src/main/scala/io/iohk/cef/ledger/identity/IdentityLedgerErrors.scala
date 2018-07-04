package io.iohk.cef.ledger.identity

import io.iohk.cef.ledger.LedgerError

class IdentityNotClaimedError(cause: Exception) extends LedgerError

object IdentityNotClaimedError {
  def apply(cause: Exception): IdentityNotClaimedError = new IdentityNotClaimedError(cause)
}

class IdentityTakenError(cause: Exception) extends LedgerError

object IdentityTakenError {
  def apply(cause: Exception): IdentityTakenError = new IdentityTakenError(cause)
}
