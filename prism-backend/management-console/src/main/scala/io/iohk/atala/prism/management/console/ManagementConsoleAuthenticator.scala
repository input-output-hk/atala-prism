package io.iohk.atala.prism.management.console

import cats.data.ReaderT
import cats.syntax.either._
import io.iohk.atala.prism.auth.AuthHelper
import io.iohk.atala.prism.auth.errors.{AuthError, UnexpectedError, UnsupportedAuthMethod}
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.{ParticipantsRepository, RequestNoncesRepository}

class ManagementConsoleAuthenticator(
    participantsRepository: ParticipantsRepository[IOWithTraceIdContext],
    requestNoncesRepository: RequestNoncesRepository[IOWithTraceIdContext]
) extends AuthHelper[ParticipantId, IOWithTraceIdContext] {

  override def burnNonce(
      id: ParticipantId,
      requestNonce: RequestNonce
  ): IOWithTraceIdContext[Unit] =
    requestNoncesRepository
      .burn(id, requestNonce)

  override def burnNonce(
      did: DID,
      requestNonce: RequestNonce
  ): IOWithTraceIdContext[Unit] =
    throw new NotImplementedError()

  override def findByPublicKey(publicKey: ECPublicKey): IOWithTraceIdContext[Either[AuthError, ParticipantId]] =
    ReaderT.pure(UnsupportedAuthMethod().asLeft[ParticipantId])

  override def findByDid(did: DID): IOWithTraceIdContext[Either[AuthError, ParticipantId]] =
    participantsRepository
      .findBy(did)
      .map(
        _.map(_.id)
          .leftMap(e => UnexpectedError(e.toStatus))
      )

}
