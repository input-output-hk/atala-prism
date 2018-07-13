package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.LedgerError
import org.bouncycastle.util.encoders.Hex

case class IdentityNotClaimedError(identity: String) extends Exception(s"Identity not claimed: ${identity}") with LedgerError

case class IdentityTakenError(identity: String) extends Exception(s"Identity already taken: ${identity}") with LedgerError

case class PublicKeyNotAssociatedWithIdentity(identity: String, publicKey: ByteString)
  extends Exception(s"Mapping doesn't exist: ${identity} -> ${Hex.toHexString(publicKey.toArray).take(8).mkString}") with LedgerError
