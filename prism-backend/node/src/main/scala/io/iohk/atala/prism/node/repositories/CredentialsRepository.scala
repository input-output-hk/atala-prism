package io.iohk.atala.prism.node.repositories

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither._
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.CredentialId
import io.iohk.atala.prism.node.models.nodeState.CredentialState
import io.iohk.atala.prism.node.repositories.daos.CredentialsDAO

class CredentialsRepository(xa: Transactor[IO]) {
  def getCredentialState(credentialId: CredentialId): FutureEither[NodeError, CredentialState] = {
    OptionT(CredentialsDAO.find(credentialId))
      .toRight(UnknownValueError("credential_id", credentialId.id))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def getCredentialTransactionInfo(credentialId: CredentialId): FutureEither[NodeError, Option[TransactionInfo]] = {
    EitherT
      .right[NodeError](CredentialsDAO.getCredentialTransactionInfo(credentialId))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }
}
