package io.iohk.cef.ledger.persistence.identity

class IdentityNotClaimedError(msg: String) extends Exception(msg)

class IdentityTakenError(msg: String) extends Exception(msg)
