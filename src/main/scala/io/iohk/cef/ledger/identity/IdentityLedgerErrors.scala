package io.iohk.cef.ledger.identity

import java.security.PublicKey

import io.iohk.cef.ledger.LedgerError
import org.bouncycastle.util.encoders.Hex

case class IdentityNotClaimedError(identity: String) extends LedgerError {
  override def toString: String = s"Identity not claimed: ${identity}"
}

case class IdentityTakenError(identity: String) extends LedgerError {
  override def toString: String = s"Identity already taken: ${identity}"
}

case class PublicKeyNotAssociatedWithIdentity(identity: String, publicKey: PublicKey) extends LedgerError {
  override def toString: String =
    s"Mapping doesn't exist: ${identity} -> ${Hex.toHexString(publicKey.getEncoded).mkString}"
}
