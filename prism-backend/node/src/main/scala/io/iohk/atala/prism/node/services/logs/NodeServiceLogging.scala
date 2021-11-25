package io.iohk.atala.prism.node.services.logs

import cats.MonadThrow
import cats.implicits.toTraverseOps
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.services._
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

import java.time.Instant

class NodeServiceLogging[F[_]: ServiceLogging[*[_], NodeService[F]]: MonadThrow] extends NodeService[Mid[F, *]] {
  override def getDidDocumentByDid(did: PrismDid): Mid[F, Either[GettingDidError, DidDocument]] = in =>
    info"getting document by the DID $did" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while getting document by the DID $err",
          _ => info"getting document by the DID - successfully done"
        )
      )
      .onError(errorCause"encountered an error while getting document by the DID" (_))

  override def getBatchState(batchId: CredentialBatchId): Mid[F, Either[errors.NodeError, BatchData]] = in =>
    info"getting batch state $batchId" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while getting batch state $err",
          _ => info"getting batch state - successfully done"
        )
      )
      .onError(errorCause"encountered an error while getting batch state" (_))

  override def getCredentialRevocationData(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): Mid[F, Either[errors.NodeError, CredentialRevocationTime]] = in =>
    info"getting credential revocation data [batchId=$batchId, credentialHash=${credentialHash.getHexValue}]" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while getting credential revocation data $err",
          _ => info"getting credential revocation data - successfully done"
        )
      )
      .onError(errorCause"encountered an error while getting credential revocation data " (_))

  override def scheduleAtalaOperations(
      ops: SignedAtalaOperation*
  ): Mid[F, List[Either[errors.NodeError, AtalaOperationId]]] = in =>
    info"scheduling atala operations" *> in
      .flatTap(_.traverse {
        _.fold(
          err => error"encountered an error while scheduling operation $err",
          operationId => info"scheduling operation $operationId - successfully done"
        )
      })
      .onError(errorCause"encountered an error while scheduling atala operations" (_))

  override def getOperationInfo(atalaOperationId: AtalaOperationId): Mid[F, OperationInfo] = in =>
    info"getting operation info $atalaOperationId" *> in
      .flatTap(_ => info"getting operation info - done")
      .onError(errorCause"encountered an error while getting operation info" (_))

  override def getLastSyncedTimestamp: Mid[F, Instant] = in =>
    info"flushing operations buffer" *> in
      .flatTap(_ => info"flushing operations buffer - done")
      .onError(errorCause"encountered an error while flushing operations buffer" (_))
}
