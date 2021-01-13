package io.iohk.atala.prism.node.repositories

import cats.data.{EitherT, OptionT}
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, LedgerData}
import io.iohk.atala.prism.node.repositories.daos.CredentialBatchesDAO
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

class CredentialBatchesRepository(xa: Transactor[IO]) {
  def getBatchState(batchId: CredentialBatchId): FutureEither[NodeError, CredentialBatchState] = {
    OptionT(CredentialBatchesDAO.findBatch(batchId))
      .toRight(UnknownValueError("batchId", batchId.id))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }

  def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): FutureEither[NodeError, Option[LedgerData]] = {
    EitherT
      .right[NodeError](CredentialBatchesDAO.findRevokedCredentialLedgerData(batchId, credentialHash))
      .transact(xa)
      .value
      .unsafeToFuture()
      .toFutureEither
  }
}
