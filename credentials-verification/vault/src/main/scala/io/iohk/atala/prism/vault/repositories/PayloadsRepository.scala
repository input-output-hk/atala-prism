package io.iohk.atala.prism.vault.repositories

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.vault.model.Payload
import io.iohk.atala.prism.vault.repositories.daos.PayloadsDAO

import scala.concurrent.ExecutionContext

class PayloadsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(id: Payload.Id): FutureEither[Nothing, Payload] = {
    PayloadsDAO
      .createPayload(id)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
