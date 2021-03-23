package io.iohk.atala.prism.node.repositories

import cats.data.EitherT
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

class CredentialBatchesRepository(xa: Transactor[IO]) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def getBatchState(batchId: CredentialBatchId): FutureEither[NodeError, Option[CredentialBatchState]] = {
    EitherT
      .right[NodeError](CredentialBatchesDAO.findBatch(batchId))
      .value
      .logSQLErrors("getting batch state", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }

  def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): FutureEither[NodeError, Option[LedgerData]] = {
    EitherT
      .right[NodeError](CredentialBatchesDAO.findRevokedCredentialLedgerData(batchId, credentialHash))
      .value
      .logSQLErrors("getting credential revocation time", logger)
      .transact(xa)
      .unsafeToFuture()
      .toFutureEither
  }
}
