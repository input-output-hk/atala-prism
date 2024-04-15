package io.iohk.atala.prism.node.services.logs

import cats.MonadThrow
import cats.implicits.toTraverseOps
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.node.models.AtalaOperationId
import io.iohk.atala.prism.node.errors
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.ProtocolVersion
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
          err => error"encountered an error while $description: $err",
          _ => info"$description - successfully done"
        )
      )
      .onError(errorCause"encountered an error while $description" (_))
  }

  override def scheduleAtalaOperations(
      ops: SignedAtalaOperation*
  ): Mid[F, List[Either[errors.NodeError, AtalaOperationId]]] = in =>
    info"scheduling Atala operations" *> in
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
          res => info"$description - done: $res"
        )
      )
      .onError(errorCause"encountered an error while $description" (_))
  }

  override def getLastSyncedTimestamp: Mid[F, Instant] = in =>
    info"getting last synced timestamp" *> in
      .flatTap(res => info"getting last synced timestamp - done: $res")
      .onError(errorCause"encountered an error while getting last synced timestamp" (_))

  override def getCurrentProtocolVersion: Mid[F, ProtocolVersion] = in =>
    info"getting current protocol version" *> in
      .flatTap(res => info"current protocol version - done: $res")
      .onError(errorCause"encountered an error while getting current protocol version" (_))

}
