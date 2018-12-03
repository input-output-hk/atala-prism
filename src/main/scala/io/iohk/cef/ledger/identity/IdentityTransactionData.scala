package io.iohk.cef.ledger.identity

import io.iohk.cef.crypto._
import io.iohk.cef.codecs.nio.auto._

sealed trait IdentityTransactionData
case class ClaimData(identity: Identity, key: SigningPublicKey) extends IdentityTransactionData {
  def toTransaction(privateKey: SigningPrivateKey): Claim = Claim(this, sign(this, privateKey))
}
case class LinkData(identity: Identity, key: SigningPublicKey) extends IdentityTransactionData {
  def toTransaction(identityPrivateKey: SigningPrivateKey, providedKeyPrivateCounterpart: SigningPrivateKey): Link =
    Link(this, sign(this, identityPrivateKey), sign(this, providedKeyPrivateCounterpart))
}
case class UnlinkData(identity: Identity, key: SigningPublicKey) extends IdentityTransactionData {
  def toTransaction(privateKey: SigningPrivateKey): Unlink = Unlink(this, sign(this, privateKey))
}
case class EndorseData(endorsingIdentity: Identity, endorsedIdentity: Identity) extends IdentityTransactionData {
  def toTransaction(endorsingIdentityPrivateKey: SigningPrivateKey): Endorse =
    Endorse(this, sign(this, endorsingIdentityPrivateKey))
}
