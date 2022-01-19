package io.iohk.atala.prism.node.repositories.logs

import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.models.nodeState
import io.iohk.atala.prism.node.repositories.CredentialBatchesRepository
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._
import cats.MonadThrow

private[repositories] final class CredentialBatchesRepositoryLogs[F[
    _
]: MonadThrow: ServiceLogging[*[
  _
], CredentialBatchesRepository[F]]]
    extends CredentialBatchesRepository[Mid[F, *]] {
  override def getBatchState(
      batchId: CredentialBatchId
  ): Mid[F, Either[errors.NodeError, Option[nodeState.CredentialBatchState]]] =
    in =>
      info"getting batch state $batchId" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while getting batch state $batchId: $err",
            res => info"getting batch state $batchId - successfully done, state found - ${res.isDefined}"
          )
        )
        .onError(errorCause"Encountered an error while getting batch state $batchId" (_))

  override def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): Mid[F, Either[errors.NodeError, Option[nodeState.LedgerData]]] =
    in =>
      info"getting credential revocation time for $batchId" *> in
        .flatTap(
          _.fold(
            err => error"Encountered an error while getting credential revocation time: $err",
            res =>
              info"getting credential revocation time for $batchId - successfully done ${res
                .map(_.transactionId)}"
          )
        )
        .onError(
          errorCause"Encountered an error while getting credential revocation time for $batchId" (
            _
          )
        )
}
