package io.iohk.atala.prism.node.repositories

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.credentials.TimestampInfo
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.CredentialId
import io.iohk.atala.prism.node.models.nodeState.CredentialState
import io.iohk.atala.prism.node.repositories.daos.CredentialsDAO
import io.iohk.atala.prism.node.repositories.daos.CredentialsDAO.CreateCredentialData

class CredentialsRepository(xa: Transactor[IO]) {
  def create(
      data: CreateCredentialData
  ): FutureEither[NodeError, Unit] = {
    val query = for {
      _ <- EitherT.right[NodeError](CredentialsDAO.insert(data))
    } yield ()

    query
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def find(credentialId: CredentialId): FutureEither[NodeError, CredentialState] = {
    OptionT(CredentialsDAO.find(credentialId))
      .toRight(UnknownValueError("credential_id", credentialId.id))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def revoke(credentialId: CredentialId, revocationTimestamp: TimestampInfo): FutureEither[NodeError, Boolean] = {
    EitherT
      .right[NodeError](CredentialsDAO.revoke(credentialId, revocationTimestamp))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }
}
