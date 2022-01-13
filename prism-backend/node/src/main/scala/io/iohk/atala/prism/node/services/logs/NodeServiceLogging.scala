package io.iohk.atala.prism.node.services.logs

import cats.MonadThrow
import cats.implicits.toTraverseOps
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.services._
import io.iohk.atala.prism.protos.node_models.{OperationOutput, SignedAtalaOperation}
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

import java.time.Instant

class NodeServiceLogging[F[_]: ServiceLogging[*[_], NodeService[F]]: MonadThrow] extends NodeService[Mid[F, *]] {
  override def getDidDocumentByDid(didStr: String): Mid[F, Either[GettingDidError, DidDocument]] = { in =>
    val description = s"getting document by the DID $didStr"
    info"$description" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while $description $err",
          _ => info"$description - successfully done"
        )
      )
      .onError(errorCause"encountered an error while $description" (_))
  }

  override def getBatchState(batchId: String): Mid[F, Either[errors.NodeError, BatchData]] = in =>
    info"getting batch state $batchId" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while getting batch state $err",
          _ => info"getting batch state - successfully done"
        )
      )
      .onError(errorCause"encountered an error while getting batch state" (_))

  override def getCredentialRevocationData(
      batchIdStr: String,
      credentialHashBS: ByteString
  ): Mid[F, Either[errors.NodeError, CredentialRevocationTime]] = in =>
    info"getting credential revocation data [batchId=$batchIdStr, credentialHash=${credentialHashBS.toByteArray.map("%02X" format _).mkString}]" *> in
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

  override def parseOperations(ops: Seq[SignedAtalaOperation]): Mid[F, Either[NodeError, List[OperationOutput]]] = in =>
    info"parsing ${ops.size} operations" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while parsing operations: $err",
          _ => info"parsing ${ops.size} operations - successfully done"
        )
      )
      .onError(errorCause"encountered an error while parsing operations" (_))

  override def getOperationInfo(atalaOperationIdBS: ByteString): Mid[F, Either[NodeError, OperationInfo]] = { in =>
    val description = s"getting operation info ${atalaOperationIdBS.toByteArray.map("%02X" format _).mkString}"
    info"$description" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while $description: $err",
          _ => info"$description - successfully done"
        )
      )
      .onError(errorCause"encountered an error while $description" (_))
  }

  override def getLastSyncedTimestamp: Mid[F, Instant] = in =>
    info"flushing operations buffer" *> in
      .flatTap(_ => info"flushing operations buffer - done")
      .onError(errorCause"encountered an error while flushing operations buffer" (_))
}
