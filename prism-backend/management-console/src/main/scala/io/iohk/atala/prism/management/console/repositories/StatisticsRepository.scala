package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.models.{ParticipantId, Statistics, TimeInterval}
import io.iohk.atala.prism.management.console.repositories.daos.StatisticsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

class StatisticsRepository(xa: Transactor[IO]) {

  def query(
      participantId: ParticipantId,
      timeIntervalMaybe: Option[TimeInterval]
  ): FutureEither[Nothing, Statistics] = {
    StatisticsDAO
      .query(participantId, timeIntervalMaybe)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }
}
