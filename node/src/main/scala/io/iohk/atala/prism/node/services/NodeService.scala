package io.iohk.atala.prism.node.services

import cats.effect.Resource
import cats.implicits._
import cats.{Applicative, Comonad, Functor, MonadThrow}
import com.google.protobuf.ByteString
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.identity.{CanonicalPrismDid, PrismDid}
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models.AtalaOperationId
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.models.{AtalaOperationInfo, ProtocolVersion}
import io.iohk.atala.prism.node.repositories.DIDDataRepository
import io.iohk.atala.prism.node.services.logs.NodeServiceLogging
import io.iohk.atala.prism.node.services.models.{getOperationOutput, validateScheduleOperationsRequest}
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{DIDData, SignedAtalaOperation}
import io.iohk.atala.prism.protos.node_api.OperationOutput
import tofu.higherKind.Mid
import tofu.logging.derivation.loggable
import tofu.logging.{Logs, ServiceLogging}

import java.time.Instant
import scala.util.Try

/** Implements logic for RPC calls in Node gRPC Server.
  */
@derive(applyK)
trait NodeService[F[_]] {

  /** Retrieves all public keys associated with `did`
    *
    * @param didStr
    *   Decentralized Identifier following PRISM protocol
    */
  def getDidDocumentByDid(didStr: String): F[Either[GettingDidError, DidDocument]]

  /** Schedules a list of operations for further publication to the underlying ledger.
    *
    * @param ops
    *   one or more operation.
    */
  def scheduleAtalaOperations(ops: node_models.SignedAtalaOperation*): F[List[Either[NodeError, AtalaOperationId]]]

  /** Parses AtalaOperations from protobuf structures to Node internal data structures.
    *
    * @param ops
    *   Sequence of SignedAtalaOperations
    * @return
    */
  def parseOperations(ops: Seq[node_models.SignedAtalaOperation]): F[Either[NodeError, List[OperationOutput]]]

  /** Retrieves information about the operation. See `OperationInfo` for the details.
    *
    * @param atalaOperationIdBS
    *   identifier of the operation.
    */
  def getOperationInfo(atalaOperationIdBS: ByteString): F[Either[NodeError, OperationInfo]]

  /** Retrieves the timestamp of the latest Blockchain block synchronized with the PRISM Node.
    */
  def getLastSyncedTimestamp: F[Instant]

  def getCurrentProtocolVersion: F[ProtocolVersion]
}

private final class NodeServiceImpl[F[_]: MonadThrow](
    didDataRepository: DIDDataRepository[F],
    objectManagement: ObjectManagementService[F]
) extends NodeService[F] {
  override def getDidDocumentByDid(didStr: String): F[Either[GettingDidError, DidDocument]] =
    Try(PrismDid.canonicalFromString(didStr)).fold(
      _ => Applicative[F].pure(UnsupportedDidFormat.asLeft[DidDocument]),
      did => getDidDocumentByDid(did)
    )

  private def getDidDocumentByDid(canon: CanonicalPrismDid): F[Either[GettingDidError, DidDocument]] = {
    val getDidResultF: F[Either[GettingDidError, Option[(DIDData, Sha256Hash)]]] =
      didDataRepository
        .findByDid(canon)
        .map(_.bimap(GettingCanonicalPrismDidError, toDidDataProto(_, canon)))

    for {
      lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
      getDidResult <- getDidResultF
      res = getDidResult.map(disResult => DidDocument(disResult.map(_._1), disResult.map(_._2), lastSyncedTimestamp))
    } yield res
  }

  private def toDidDataProto(in: Option[DIDDataState], canon: CanonicalPrismDid): Option[(DIDData, Sha256Hash)] =
    in.map(didDataState => (ProtoCodecs.toDIDDataProto(canon.getSuffix, didDataState), didDataState.lastOperation))

  override def parseOperations(ops: Seq[SignedAtalaOperation]): F[Either[NodeError, List[OperationOutput]]] =
    Applicative[F].pure {
      for {
        // pre-validation of the complete batch
        _ <- validateScheduleOperationsRequest(ops)
        // parse and validate operations with mock data
        operationOutputs <- ops
          .traverse(getOperationOutput)
          .left
          .map { err =>
            NodeError.InvalidArgument(err.render)
          }
      } yield operationOutputs.toList
    }

  override def scheduleAtalaOperations(ops: SignedAtalaOperation*): F[List[Either[NodeError, AtalaOperationId]]] =
    objectManagement.scheduleAtalaOperations(ops: _*)

  override def getOperationInfo(atalaOperationIdBS: ByteString): F[Either[NodeError, OperationInfo]] =
    for {
      atalaOperationIdE <- Applicative[F].pure(
        Try(AtalaOperationId.fromVectorUnsafe(atalaOperationIdBS.toByteArray.toVector)).toEither.left
          .map(err => NodeError.InvalidArgument(err.getMessage): NodeError)
      )
      atalaOperationInfoE <- atalaOperationIdE.fold(
        err => Applicative[F].pure(err.asLeft[OperationInfo]),
        opId => getOperationInfo(opId).map(_.asRight[NodeError])
      )
    } yield atalaOperationInfoE

  private def getOperationInfo(atalaOperationId: AtalaOperationId): F[OperationInfo] = for {
    lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
    maybeOperationInfo <- objectManagement.getOperationInfo(atalaOperationId)
    result = OperationInfo(maybeOperationInfo, lastSyncedTimestamp)
  } yield result

  override def getLastSyncedTimestamp: F[Instant] = objectManagement.getLastSyncedTimestamp

  override def getCurrentProtocolVersion: F[ProtocolVersion] = objectManagement.getCurrentProtocolVersion
}

object NodeService {

  def make[I[_]: Functor, F[_]: MonadThrow](
      didDataRepository: DIDDataRepository[F],
      objectManagement: ObjectManagementService[F],
      logs: Logs[I, F]
  ): I[NodeService[F]] = {
    for {
      serviceLogs <- logs.service[NodeService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, NodeService[F]] = serviceLogs
      val logs: NodeService[Mid[F, *]] = new NodeServiceLogging[F]
      val mid: NodeService[Mid[F, *]] = logs
      mid attach new NodeServiceImpl[F](
        didDataRepository,
        objectManagement
      )
    }
  }

  def resource[I[_]: Comonad, F[_]: MonadThrow](
      didDataRepository: DIDDataRepository[F],
      objectManagement: ObjectManagementService[F],
      logs: Logs[I, F]
  ): Resource[I, NodeService[F]] = Resource.eval(
    make(didDataRepository, objectManagement, logs)
  )

  def unsafe[I[_]: Comonad, F[_]: MonadThrow](
      didDataRepository: DIDDataRepository[F],
      objectManagement: ObjectManagementService[F],
      logs: Logs[I, F]
  ): NodeService[F] =
    make(
      didDataRepository,
      objectManagement,
      logs
    ).extract

}

final case class DidDocument(
    maybeData: Option[DIDData],
    maybeOperation: Option[Sha256Hash],
    lastSyncedTimeStamp: Instant
)

@derive(loggable)
final case class OperationInfo(maybeOperationInfo: Option[AtalaOperationInfo], lastSyncedTimestamp: Instant)

@derive(loggable)
sealed trait GettingDidError

final case class GettingCanonicalPrismDidError(node: NodeError) extends GettingDidError

case object UnsupportedDidFormat extends GettingDidError
