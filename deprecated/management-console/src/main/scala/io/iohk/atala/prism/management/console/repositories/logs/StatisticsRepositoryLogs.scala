package io.iohk.atala.prism.management.console.repositories.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.management.console.models.{ParticipantId, Statistics, TimeInterval}
import io.iohk.atala.prism.management.console.repositories.StatisticsRepository
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.effect.MonadCancelThrow

private[repositories] final class StatisticsRepositoryLogs[F[_]: ServiceLogging[
  *[_],
  StatisticsRepository[F]
]: MonadCancelThrow]
    extends StatisticsRepository[Mid[F, *]] {
  override def query(
      participantId: ParticipantId,
      timeIntervalMaybe: Option[TimeInterval]
  ): Mid[F, Statistics] =
    in =>
      info"getting statistics $participantId" *> in
        .flatTap(_ => info"getting statistics - successfully done")
        .onError(errorCause"encountered an error while getting statistics" (_))

}
