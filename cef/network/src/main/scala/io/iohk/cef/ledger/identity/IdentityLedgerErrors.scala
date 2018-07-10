package io.iohk.cef.ledger.identity

import io.iohk.cef.ledger.LedgerError

case class IdentityNotClaimedError(identity: String) extends Exception(s"Identity not claimed: ${identity}") with LedgerError

case class IdentityTakenError(identity: String) extends Exception(s"Identity already taken: ${identity}") with LedgerError

case class PublicKeyNotAssociatedWithIdentity(identity: String, publicKey: String) extends Exception(s"Mapping doesn't exist: ${identity} -> ${publicKey}") with LedgerError
