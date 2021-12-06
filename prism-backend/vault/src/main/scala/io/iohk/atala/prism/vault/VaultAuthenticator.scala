package io.iohk.atala.prism.vault

import cats.Monad
import cats.data.ReaderT
import io.iohk.atala.prism.auth.AuthHelper
import io.iohk.atala.prism.auth.errors.{AuthError, UnsupportedAuthMethod}
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext

class VaultAuthenticator extends AuthHelper[Unit, IOWithTraceIdContext] {

  override def burnNonce(
      unit: Unit,
      requestNonce: RequestNonce
  ): IOWithTraceIdContext[Unit] =
    Monad[IOWithTraceIdContext].unit

  override def burnNonce(
      unit: DID,
      requestNonce: RequestNonce
  ): IOWithTraceIdContext[Unit] =
    Monad[IOWithTraceIdContext].unit

  override def findByPublicKey(publicKey: ECPublicKey): IOWithTraceIdContext[Either[AuthError, Unit]] =
    ReaderT.pure(Left(UnsupportedAuthMethod()))

  override def findByDid(did: DID): IOWithTraceIdContext[Either[AuthError, Unit]] =
    ReaderT.pure(Left(UnsupportedAuthMethod()))
}
