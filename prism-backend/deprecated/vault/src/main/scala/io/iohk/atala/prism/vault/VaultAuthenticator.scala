package io.iohk.atala.prism.vault

import cats.data.ReaderT
import io.iohk.atala.prism.auth.AuthHelper
import io.iohk.atala.prism.auth.errors.{AuthError, UnsupportedAuthMethod}
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.vault.repositories.RequestNoncesRepository

class VaultAuthenticator(
    requestNoncesRepository: RequestNoncesRepository[IOWithTraceIdContext]
) extends AuthHelper[DID, IOWithTraceIdContext] {

  override def burnNonce(
      did: DID,
      requestNonce: RequestNonce
  ): IOWithTraceIdContext[Unit] =
    requestNoncesRepository
      .burn(did, requestNonce)

  override def findByPublicKey(publicKey: ECPublicKey): IOWithTraceIdContext[Either[AuthError, DID]] =
    ReaderT.pure(Left(UnsupportedAuthMethod()))

  override def findByDid(did: DID): IOWithTraceIdContext[Either[AuthError, DID]] =
    ReaderT.pure(Right(did))
}
