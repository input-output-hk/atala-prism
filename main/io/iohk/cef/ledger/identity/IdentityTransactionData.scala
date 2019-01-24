package io.iohk.cef.ledger.identity

import io.iohk.cef.crypto._

sealed trait IdentityTransactionData
case class ClaimData(identity: Identity, key: SigningPublicKey) extends IdentityTransactionData
case class LinkData(identity: Identity, key: SigningPublicKey) extends IdentityTransactionData
case class UnlinkData(identity: Identity, key: SigningPublicKey) extends IdentityTransactionData
case class EndorseData(endorserIdentity: Identity, endorsedIdentity: Identity) extends IdentityTransactionData
case class RevokeEndorsementData(endorserIdentity: Identity, endorsedIdentity: Identity) extends IdentityTransactionData
case class GrantData(grantingIdentity: Identity, grantedIdentity: Identity, grantedIdentityPublicKey: SigningPublicKey)
    extends IdentityTransactionData {
  val underlyingClaimData = ClaimData(grantedIdentity, grantedIdentityPublicKey)
  val underlyingEndorseData = EndorseData(grantingIdentity, grantedIdentity)
}
case class LinkCertificateData(linkingIdentity: Identity, pem: String) extends IdentityTransactionData
