package io.iohk.cvp.cstore.services

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cstore.models.StoredCredential
import io.iohk.cvp.cstore.repositories.daos.StoredCredentialsDAO
import io.iohk.cvp.cstore.repositories.daos.StoredCredentialsDAO.StoredCredentialCreateData
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class StoredCredentialsService(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def getCredentialsFor(
      userId: ParticipantId,
      individualId: ParticipantId
  ): FutureEither[Nothing, Seq[StoredCredential]] = {
    StoredCredentialsDAO
      .getFor(userId, individualId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storeCredential(userId: ParticipantId, data: StoredCredentialCreateData): FutureEither[Nothing, Unit] = {
    StoredCredentialsDAO
      .insert(userId, data)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
