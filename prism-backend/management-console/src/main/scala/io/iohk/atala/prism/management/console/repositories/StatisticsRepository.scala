package io.iohk.atala.prism.management.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.management.console.models.{ParticipantId, Statistics, TimeInterval}
import io.iohk.atala.prism.management.console.repositories.daos.StatisticsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

class StatisticsRepository(xa: Transactor[IO]) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def query(
      participantId: ParticipantId,
      timeIntervalMaybe: Option[TimeInterval]
  ): FutureEither[Nothing, Statistics] = {
    StatisticsDAO
      .query(participantId, timeIntervalMaybe)
      .logSQLErrors(s"getting statistics, participant id - $participantId", logger)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }
}
