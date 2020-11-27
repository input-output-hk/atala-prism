package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.models.{ParticipantId, Statistics}
import io.iohk.atala.prism.management.console.repositories.daos.StatisticsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

class StatisticsRepository(xa: Transactor[IO]) {

  def query(participantId: ParticipantId): FutureEither[Nothing, Statistics] = {
    StatisticsDAO
      .query(participantId)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }
}
