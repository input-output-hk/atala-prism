package io.iohk.atala.prism.connector

import cats.syntax.either._
import io.iohk.atala.prism.auth
import io.iohk.atala.prism.auth.AuthHelper
import io.iohk.atala.prism.auth.errors.{AuthError, UnexpectedError}
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.ParticipantId

class ConnectorAuthenticatorF(
    participantsRepository: ParticipantsRepository[IOWithTraceIdContext],
    requestNoncesRepository: RequestNoncesRepository[IOWithTraceIdContext]
) extends AuthHelper[ParticipantId, IOWithTraceIdContext] {

  override def burnNonce(
      id: ParticipantId,
      requestNonce: auth.model.RequestNonce
  ): IOWithTraceIdContext[Unit] =
    requestNoncesRepository
      .burn(id, requestNonce)

  override def burnNonce(
      did: DID,
      requestNonce: RequestNonce
  ): IOWithTraceIdContext[Unit] =
    requestNoncesRepository
      .burn(did, requestNonce)

  override def findByPublicKey(
      publicKey: ECPublicKey
  ): IOWithTraceIdContext[Either[AuthError, ParticipantId]] =
    participantsRepository
      .findBy(publicKey)
      .map(
        _.leftMap(_.unify)
          .map(_.id)
          .leftMap(e => UnexpectedError(e.toStatus))
      )

  override def findByDid(did: DID): IOWithTraceIdContext[Either[AuthError, ParticipantId]] =
    participantsRepository
      .findBy(did)
      .map(
        _.leftMap(_.unify)
          .map(_.id)
          .leftMap(e => UnexpectedError(e.toStatus))
      )
}
