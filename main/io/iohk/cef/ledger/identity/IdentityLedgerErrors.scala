package io.iohk.cef.ledger.identity

import io.iohk.crypto._
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

case class IdentityTakenError(identity: String) extends LedgerError {
  override def toString: String = s"Identity already taken: ${identity}"
}

case class PublicKeyNotAssociatedWithIdentity(identity: String, publicKey: SigningPublicKey) extends LedgerError {
  override def toString: String =
    s"Mapping doesn't exist: $identity -> ${toHexString(publicKey.toByteString)}"
}
case class EndorsementNotAssociatedWithIdentityError(identity: Identity, endorsedIdentity: Identity)
    extends LedgerError {
  override def toString: String =
    s"Endorsement Mapping doesn't exist: $identity -> $endorsedIdentity"
}

case class LinkingIdentitySignatureRequiredError(identity: String, publicKey: SigningPublicKey) extends LedgerError {
  override def toString: String =
    s"Signature required for linking identity: $identity -> ${toHexString(publicKey.toByteString)}"
}

case class IdentityIsNotAGrantingAuthorityError(identity: Identity) extends LedgerError {
  override def toString: String =
    s"Identity ${identity} provided must be a granting authority to perform this action."
}

//Intrinsic validation errors

case class UnableToVerifySignatureException(identity: String)
  extends IllegalArgumentException(s"The given identity: ${identity} , signature can't be verified with the associated public key.")

case class InvalidCertificateException()
  extends IllegalArgumentException("The given certificate is invalid, it must have two certificates with different identities")

case class IdentityNotMatchingCertificateException(linkingIdentity: Identity, certificateIdentity: Identity)
  extends IllegalArgumentException(s"The linking identity = [$linkingIdentity] doesn't match the certificate = [$certificateIdentity]")

case class InvalidSignatureException()
  extends IllegalArgumentException(s"The provided signature is invalid.")
