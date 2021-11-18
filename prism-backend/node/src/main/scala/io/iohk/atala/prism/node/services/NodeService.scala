package io.iohk.atala.prism.node.services

import cats.effect.Resource
import cats.effect.kernel.Sync
import cats.{Applicative, Comonad, Functor}
import cats.implicits._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.identity.{CanonicalPrismDid, LongFormPrismDid, PrismDid}
import io.iohk.atala.prism.interop.toScalaProtos.AtalaOperationInterop
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.AtalaOperationInfo
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, DIDDataState, LedgerData}
import io.iohk.atala.prism.node.repositories.{CredentialBatchesRepository, DIDDataRepository}
import io.iohk.atala.prism.node.services.logs.NodeServiceLogging
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{DIDData, SignedAtalaOperation}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.logging.derivation.loggable

import java.time.Instant

@derive(applyK)
trait NodeService[F[_]] {

  def getDidDocumentByDid(did: PrismDid): F[Either[GettingDidError, DidDocument]]

  def scheduleOperation(op: SignedAtalaOperation): F[Either[NodeError, AtalaOperationId]]

  def getBatchState(batchId: CredentialBatchId): F[Either[NodeError, BatchData]]

  def getCredentialRevocationData(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): F[Either[NodeError, CredentialRevocationTime]]

  def scheduleAtalaOperations(ops: node_models.SignedAtalaOperation*): F[List[Either[NodeError, AtalaOperationId]]]

  def getOperationInfo(atalaOperationId: AtalaOperationId): F[OperationInfo]

  def flushOperationsBuffer: F[Unit]

  def getLastSyncedTimestamp: F[Instant]

}
// Please forgive me for that Sync here -_-
// It's needed for delaying of submissionSchedulingService.flushOperationsBuffer() call
private final class NodeServiceImpl[F[_]: Sync](
    didDataRepository: DIDDataRepository[F],
    objectManagement: ObjectManagementService[F],
    credentialBatchesRepository: CredentialBatchesRepository[F],
    submissionSchedulingService: SubmissionSchedulingService
) extends NodeService[F] {
  override def getDidDocumentByDid(did: PrismDid): F[Either[GettingDidError, DidDocument]] = {
    val getDidResultF: F[Either[GettingDidError, Option[DIDData]]] = did match {
      case canon: CanonicalPrismDid =>
        didDataRepository.findByDid(canon).map(_.bimap(GettingCanonicalPrismDidError, toDidDataProto(_, canon)))
      case longForm: LongFormPrismDid =>
        didDataRepository.findByDid(did.asCanonical()).map(handleFindByLongFormResult(_, longForm, did))
      case _ => Applicative[F].pure(UnsupportedDidFormat.asLeft)
    }
    for {
      lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
      getDidResult <- getDidResultF
      res = getDidResult.map(DidDocument(_, lastSyncedTimestamp))
    } yield res
  }

  private def handleFindByLongFormResult(
      result: Either[NodeError, Option[DIDDataState]],
      longForm: LongFormPrismDid,
      did: PrismDid
  ): Either[GettingDidError, Option[DIDData]] = {
    def returnInitialState: Option[DIDData] =
      ProtoCodecs.atalaOperationToDIDDataProto(DidSuffix(did.getSuffix), longForm.getInitialState.asScala).some
    // if it was not published or we have an error, we return the encoded initial state
    result.fold(
      _ => returnInitialState.asRight[GettingDidError],
      _.fold(returnInitialState.asRight[GettingDidError])(state =>
        ProtoCodecs.toDIDDataProto(did.getSuffix, state).some.asRight[GettingDidError]
      )
    )
  }

  private def toDidDataProto(in: Option[DIDDataState], canon: CanonicalPrismDid): Option[DIDData] =
    in.map(didDataState => ProtoCodecs.toDIDDataProto(canon.getSuffix, didDataState))

  override def scheduleOperation(op: SignedAtalaOperation): F[Either[NodeError, AtalaOperationId]] =
    objectManagement.scheduleSingleAtalaOperation(op)

  override def getBatchState(batchId: CredentialBatchId): F[Either[NodeError, BatchData]] =
    for {
      lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
      maybeBatchStateE <- credentialBatchesRepository.getBatchState(batchId)
      batchData = maybeBatchStateE.map(BatchData(_, lastSyncedTimestamp))
    } yield batchData

  override def getCredentialRevocationData(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): F[Either[NodeError, CredentialRevocationTime]] =
    for {
      lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
      maybeTime <- credentialBatchesRepository.getCredentialRevocationTime(batchId, credentialHash)
      credentialRevocationTime = maybeTime.map(CredentialRevocationTime(_, lastSyncedTimestamp))
    } yield credentialRevocationTime

  override def scheduleAtalaOperations(ops: SignedAtalaOperation*): F[List[Either[NodeError, AtalaOperationId]]] =
    objectManagement.scheduleAtalaOperations(ops: _*)

  override def getOperationInfo(atalaOperationId: AtalaOperationId): F[OperationInfo] = for {
    lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
    maybeOperationInfo <- objectManagement.getOperationInfo(atalaOperationId)
    result = OperationInfo(maybeOperationInfo, lastSyncedTimestamp)
  } yield result

  override def flushOperationsBuffer: F[Unit] = Sync[F].delay(submissionSchedulingService.flushOperationsBuffer())

  override def getLastSyncedTimestamp: F[Instant] = objectManagement.getLastSyncedTimestamp
}

object NodeService {

  def make[I[_]: Functor, F[_]: Sync](
      didDataRepository: DIDDataRepository[F],
      objectManagement: ObjectManagementService[F],
      credentialBatchesRepository: CredentialBatchesRepository[F],
      submissionSchedulingService: SubmissionSchedulingService,
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
        objectManagement,
        credentialBatchesRepository,
        submissionSchedulingService
      )
    }
  }

  def resource[I[_]: Comonad, F[_]: Sync](
      didDataRepository: DIDDataRepository[F],
      objectManagement: ObjectManagementService[F],
      credentialBatchesRepository: CredentialBatchesRepository[F],
      submissionSchedulingService: SubmissionSchedulingService,
      logs: Logs[I, F]
  ): Resource[I, NodeService[F]] = Resource.eval(
    make(didDataRepository, objectManagement, credentialBatchesRepository, submissionSchedulingService, logs)
  )

  def unsafe[I[_]: Comonad, F[_]: Sync](
      didDataRepository: DIDDataRepository[F],
      objectManagement: ObjectManagementService[F],
      credentialBatchesRepository: CredentialBatchesRepository[F],
      submissionSchedulingService: SubmissionSchedulingService,
      logs: Logs[I, F]
  ): NodeService[F] =
    make(didDataRepository, objectManagement, credentialBatchesRepository, submissionSchedulingService, logs).extract

}

final case class BatchData(maybeBatchState: Option[CredentialBatchState], lastSyncedTimestamp: Instant)

final case class CredentialRevocationTime(maybeLedgerData: Option[LedgerData], lastSyncedTimestamp: Instant)

final case class DidDocument(maybeData: Option[DIDData], lastSyncedTimeStamp: Instant)

final case class OperationInfo(maybeOperationInfo: Option[AtalaOperationInfo], lastSyncedTimestamp: Instant)

@derive(loggable)
sealed trait GettingDidError

final case class GettingCanonicalPrismDidError(node: NodeError) extends GettingDidError

case object UnsupportedDidFormat extends GettingDidError
