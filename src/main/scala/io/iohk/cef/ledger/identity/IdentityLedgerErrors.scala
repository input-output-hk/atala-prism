package io.iohk.cef.ledger.identity

import io.iohk.cef.crypto._
import io.iohk.cef.ledger.LedgerError
import org.bouncycastle.util.encoders.Hex

case class IdentityNotClaimedError(identity: String) extends LedgerError {
  override def toString: String = s"Identity not claimed: ${identity}"
}

case object UnableToVerifySignatureError extends LedgerError {
  override def toString: String = s"The given signature can't be verified"
}

case class IdentityTakenError(identity: String) extends LedgerError {
  override def toString: String = s"Identity already taken: ${identity}"
}

case class PublicKeyNotAssociatedWithIdentity(identity: String, publicKey: SigningPublicKey) extends LedgerError {
  override def toString: String =
    s"Mapping doesn't exist: $identity -> ${Hex.toHexString(publicKey.toByteString.toArray).mkString}"
}
