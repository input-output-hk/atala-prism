package io.iohk.atala.prism.node.services

import cats.effect.kernel.Sync
import cats.Applicative
import cats.implicits._
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
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{DIDData, SignedAtalaOperation}

import java.time.Instant

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
class NodeServiceImpl[F[_]: Sync](
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
      getDidResult <- getDidResultF
      res <- getDidResult.traverse(maybeData => objectManagement.getLastSyncedTimestamp.map(DidDocument(maybeData, _)))
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
      maybeBatchStateE <- credentialBatchesRepository.getBatchState(batchId)
      result <- maybeBatchStateE.traverse(maybeBatchState =>
        objectManagement.getLastSyncedTimestamp.map(BatchData(maybeBatchState, _))
      )
    } yield result

  override def getCredentialRevocationData(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): F[Either[NodeError, CredentialRevocationTime]] =
    for {
      maybeTime <- credentialBatchesRepository.getCredentialRevocationTime(batchId, credentialHash)
      lastSyncedTimestamp <- maybeTime.traverse(maybeLedgerData =>
        objectManagement.getLastSyncedTimestamp.map(CredentialRevocationTime(maybeLedgerData, _))
      )
    } yield lastSyncedTimestamp

  override def scheduleAtalaOperations(ops: SignedAtalaOperation*): F[List[Either[NodeError, AtalaOperationId]]] =
    objectManagement.scheduleAtalaOperations(ops: _*)

  override def getOperationInfo(atalaOperationId: AtalaOperationId): F[OperationInfo] = for {
    maybeOperationInfo <- objectManagement.getOperationInfo(atalaOperationId)
    lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
    result = OperationInfo(maybeOperationInfo, lastSyncedTimestamp)
  } yield result

  override def flushOperationsBuffer: F[Unit] = Sync[F].delay(submissionSchedulingService.flushOperationsBuffer())

  override def getLastSyncedTimestamp: F[Instant] = objectManagement.getLastSyncedTimestamp
}

final case class BatchData(maybeBatchState: Option[CredentialBatchState], lastSyncedTimestamp: Instant)

final case class CredentialRevocationTime(maybeLedgerData: Option[LedgerData], lastSyncedTimestamp: Instant)

final case class DidDocument(maybeData: Option[DIDData], lastSyncedTimeStamp: Instant)

final case class OperationInfo(maybeOperationInfo: Option[AtalaOperationInfo], lastSyncedTimestamp: Instant)

sealed trait GettingDidError

final case class GettingCanonicalPrismDidError(node: NodeError) extends GettingDidError

case object UnsupportedDidFormat extends GettingDidError
