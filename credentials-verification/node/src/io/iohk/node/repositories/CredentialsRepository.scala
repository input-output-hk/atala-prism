package io.iohk.node.repositories

import java.sql.Date

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._
import io.iohk.node.errors.NodeError
import io.iohk.node.errors.NodeError.UnknownValueError
import io.iohk.node.models.{Credential, CredentialId}
import io.iohk.node.repositories.daos.CredentialsDAO
import io.iohk.node.repositories.daos.CredentialsDAO.CreateCredentialData

import scala.concurrent.ExecutionContext

class CredentialsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
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

  def find(credentialId: CredentialId): FutureEither[NodeError, Credential] = {
    OptionT(CredentialsDAO.find(credentialId))
      .toRight(UnknownValueError("credential_id", credentialId.id))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def revoke(credentialId: CredentialId, revocationDate: Date): FutureEither[NodeError, Boolean] = {
    EitherT
      .right[NodeError](CredentialsDAO.revoke(credentialId, revocationDate))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }
}
