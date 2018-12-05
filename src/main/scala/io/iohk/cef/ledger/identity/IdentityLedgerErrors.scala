package io.iohk.cef.ledger.identity

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.LedgerError
import io.iohk.cef.utils.HexStringCodec._

case class IdentityNotClaimedError(identity: String) extends LedgerError {
  override def toString: String = s"Identity not claimed: ${identity}"
}

case class UnknownEndorsedIdentityError(identity: String) extends LedgerError {
  override def toString: String = s"Identity to endorse doesn't exists : ${identity}"
}

case class UnknownEndorserIdentityError(identity: String) extends LedgerError {
  override def toString: String = s"Identity of endorser doesn't exists : ${identity}"
}

case class UnableToVerifyEndorserSignatureError(identity: String, endorserSignature: Signature) extends LedgerError {
  override def toString: String =
    s"The given identity: ${identity} , signature can't be verified with the associated public key : ${toHexString(endorserSignature.toByteString)}"
}

case object UnableToVerifySignatureError extends LedgerError {
  override def toString: String = s"The given signature can't be verified"
}

case class UnableToVerifyLinkingIdentitySignatureError(identity: String, publicKey: SigningPublicKey)
    extends LedgerError {
  override def toString: String =
    s"The given identity: ${identity} , signature can't be verified with the associated public key : ${toHexString(publicKey.toByteString)}"
}

case class IdentityTakenError(identity: String) extends LedgerError {
  override def toString: String = s"Identity already taken: ${identity}"
}

case class PublicKeyNotAssociatedWithIdentity(identity: String, publicKey: SigningPublicKey) extends LedgerError {
  override def toString: String =
    s"Mapping doesn't exist: $identity -> ${toHexString(publicKey.toByteString)}"
}
case class EndorsementNotAssociatedWithIdentityError(identity: Identity, endorsedIdentity: Identity) extends LedgerError {
  override def toString: String =
    s"Endorsement Mapping doesn't exist: $identity -> $endorsedIdentity"
}

case class LinkingIdentitySignatureRequiredError(identity: String, publicKey: SigningPublicKey) extends LedgerError {
  override def toString: String =
    s"Signature required for linking identity: $identity -> ${toHexString(publicKey.toByteString)}"
}
