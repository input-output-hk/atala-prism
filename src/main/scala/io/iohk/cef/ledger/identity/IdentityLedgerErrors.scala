package io.iohk.cef.ledger.identity

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.LedgerError
import io.iohk.cef.utils.HexStringCodec._

case class IdentityNotClaimedError(identity: String) extends LedgerError {
  override def toString: String = s"Identity not claimed: ${identity}"
}

case object UnableToVerifySignatureError extends LedgerError {
  override def toString: String = s"The given signature can't be verified"
}

case class UnableToVerifyLinkingIdentitySignatureError(identity: String, publicKey: SigningPublicKey)
    extends LedgerError {
  override def toString: String =
    s"The given identity: ${identity} , signature can't be verified with the associated public key : ${publicKey}"
}

case class IdentityTakenError(identity: String) extends LedgerError {
  override def toString: String = s"Identity already taken: ${identity}"
}

case class PublicKeyNotAssociatedWithIdentity(identity: String, publicKey: SigningPublicKey) extends LedgerError {
  override def toString: String =
    s"Mapping doesn't exist: $identity -> ${toHexString(publicKey.toByteString)}"
}

case class LinkingIdentitySignatureRequiredError(identity: String, publicKey: SigningPublicKey) extends LedgerError {
  override def toString: String =
    s"Signature required for linking identity: $identity -> ${toHexString(publicKey.toByteString)}"
}
