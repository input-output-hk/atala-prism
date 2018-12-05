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
case class EndorseData(endorserIdentity: Identity, endorsedIdentity: Identity) extends IdentityTransactionData {
  def toTransaction(endorserIdentityPrivateKey: SigningPrivateKey): Endorse =
    Endorse(this, sign(this, endorserIdentityPrivateKey))
}
case class GrantData(grantingIdentity: Identity, grantedIdentity: Identity, grantedIdentityPublicKey: SigningPublicKey)
    extends IdentityTransactionData {
  val underlyingClaimData = ClaimData(grantedIdentity, grantedIdentityPublicKey)
  val underlyingEndorseData = EndorseData(grantingIdentity, grantedIdentity)
  def toTransaction(
      grantingIdentityPrivateKey: SigningPrivateKey,
      grantedIdentityPrivateKey: SigningPrivateKey): Grant = {
    val grantSignature = sign(this, grantingIdentityPrivateKey)
    val claimSignature = sign(underlyingClaimData, grantedIdentityPrivateKey)
    val endorseSignature = sign(underlyingEndorseData, grantingIdentityPrivateKey)
    Grant(this, grantSignature, claimSignature, endorseSignature)
  }
}
