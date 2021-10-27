package io.iohk.atala.prism.management.console.repositories.logs

import cats.effect.BracketThrow
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.management.console.models.ParticipantId
import io.iohk.atala.prism.management.console.repositories.RequestNoncesRepository
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

private[repositories] final class RequestNoncesRepositoryLogs[F[
    _
]: ServiceLogging[
  *[_],
  RequestNoncesRepository[F]
]: BracketThrow]
    extends RequestNoncesRepository[Mid[F, *]] {
  override def burn(
      participantId: ParticipantId,
      requestNonce: RequestNonce
  ): Mid[F, Unit] =
    in =>
      info"burning $participantId" *> in
        .flatTap(_ => info"burning - successfully done")
        .onError(errorCause"encountered an error while burning" (_))
}
