package io.iohk.atala.prism.console.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.console.models.{Institution, Statistics}
import io.iohk.atala.prism.console.repositories.daos.StatisticsDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

class StatisticsRepository(xa: Transactor[IO]) {

  def query(institutionId: Institution.Id): FutureEither[Nothing, Statistics] = {
    StatisticsDAO
      .query(institutionId)
      .transact(xa)
      .map(Right(_))
      .unsafeToFuture()
      .toFutureEither
  }
}
