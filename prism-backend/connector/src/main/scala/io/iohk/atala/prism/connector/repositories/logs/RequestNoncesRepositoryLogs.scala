package io.iohk.atala.prism.connector.repositories.logs

import cats.effect.MonadThrow
import cats.syntax.apply._
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import io.iohk.atala.prism.auth.model
import io.iohk.atala.prism.connector.repositories.RequestNoncesRepository
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.ParticipantId
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] class RequestNoncesRepositoryLogs[
    F[_]: ServiceLogging[*[_], RequestNoncesRepository[F]]
](implicit
    monadThrow: MonadThrow[F]
) extends RequestNoncesRepository[Mid[F, *]] {
  override def burn(
      participantId: ParticipantId,
      requestNonce: model.RequestNonce
  ): Mid[F, Unit] =
    in =>
      info"burning nonce $participantId" *> in
        .flatMap(_ => info"burning nonce - successfully done")
        .onError(errorCause"encountered an error while burning nonce" (_))

  override def burn(did: DID, requestNonce: model.RequestNonce): Mid[F, Unit] =
    in =>
      info"burning nonce ${did.getSuffix}" *> in
        .flatMap(_ => info"burning nonce - successfully done")
        .onError(errorCause"encountered an error while burning nonce" (_))
}
