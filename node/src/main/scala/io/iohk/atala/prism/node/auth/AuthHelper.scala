package io.iohk.atala.prism.node.auth

import io.iohk.atala.prism.node.auth.errors.AuthError
import io.iohk.atala.prism.node.auth.model.RequestNonce
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey

trait WhitelistedAuthHelper[F[_]] {

  /** Burns given nonce for DID, so that the request can not be cloned by a malicious agent
    */
  def burnNonce(did: DID, requestNonce: RequestNonce): F[Unit]
}

trait AuthHelper[Id, F[_]] extends WhitelistedAuthHelper[F] {

  /** Burns given nonce for user id, so that the request can not be cloned by a malicious agent
    */
  def burnNonce(id: Id, requestNonce: RequestNonce): F[Unit]

  /** Finds a user associated with the given public key
    */
  def findByPublicKey(publicKey: SecpPublicKey): F[Either[AuthError, Id]]

  /** Finds a user associated with the given DID
    */
  def findByDid(did: DID): F[Either[AuthError, Id]]
}
