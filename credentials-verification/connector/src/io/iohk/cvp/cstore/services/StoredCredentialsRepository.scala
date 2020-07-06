package io.iohk.cvp.cstore.services

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.cstore.models.StoredSignedCredential
import io.iohk.cvp.cstore.repositories.daos.StoredCredentialsDAO
import io.iohk.cvp.cstore.repositories.daos.StoredCredentialsDAO.StoredSignedCredentialData
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class StoredCredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def getCredentialsFor(
      verifierId: ParticipantId,
      individualId: ParticipantId
  ): FutureEither[Nothing, Seq[StoredSignedCredential]] = {
    StoredCredentialsDAO
      .getStoredCredentialsFor(verifierId, individualId)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def storeCredential(data: StoredSignedCredentialData): FutureEither[Nothing, Unit] = {
    StoredCredentialsDAO
      .storeSignedCredential(data)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }
}
