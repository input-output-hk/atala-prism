package io.iohk.atala.prism.vault.repositories

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.vault.model.{CreatePayload, Payload}
import io.iohk.atala.prism.vault.repositories.daos.PayloadsDAO

import scala.concurrent.ExecutionContext

class PayloadsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(payloadData: CreatePayload): FutureEither[Nothing, Payload] = {
    PayloadsDAO
      .createPayload(payloadData)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def getByPaginated(
      did: DID,
      lastSeenCreationOrder: Option[Payload.Id],
      limit: Int
  ): FutureEither[Nothing, List[Payload]] = {
    PayloadsDAO
      .getByPaginated(did, lastSeenCreationOrder, limit)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
