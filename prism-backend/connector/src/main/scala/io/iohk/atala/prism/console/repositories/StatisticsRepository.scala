package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.console.models.{Institution, Statistics}
import io.iohk.atala.prism.console.repositories.daos.StatisticsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

class StatisticsRepository(xa: Transactor[IO]) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def query(institutionId: Institution.Id): FutureEither[Nothing, Statistics] = {
    StatisticsDAO
      .query(institutionId)
      .logSQLErrors(s"getting statistics, institution id - $institutionId", logger)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }
}
