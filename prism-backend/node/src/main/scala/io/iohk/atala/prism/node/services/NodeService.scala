package io.iohk.atala.prism.node.services

import cats.effect.Resource
import cats.implicits._
import cats.{Applicative, Comonad, Functor, MonadThrow}
import com.google.protobuf.ByteString
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.identity.{CanonicalPrismDid, PrismDid}
import io.iohk.atala.prism.models.{TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.UnderlyingLedger
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.InvalidArgument
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.{AtalaOperationInfo, ProtocolVersion, Balance}
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, DIDDataState, LedgerData}
import io.iohk.atala.prism.node.repositories.{CredentialBatchesRepository, DIDDataRepository}
import io.iohk.atala.prism.node.services.logs.NodeServiceLogging
import io.iohk.atala.prism.node.services.models.{getOperationOutput, validateScheduleOperationsRequest}
import io.iohk.atala.prism.protos.node_api.GetWalletTransactionsRequest
import io.iohk.atala.prism.protos.node_models.{DIDData, OperationOutput, SignedAtalaOperation}
import io.iohk.atala.prism.protos.{node_api, node_models}
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

  /** Get information about credentials batch identified by `batchId`. See `BatchData` for the details.
    *
    * @param batchId
    *   identifier of the credentials batch.
    */
  def getBatchState(batchId: String): F[Either[NodeError, BatchData]]

  /** Retrieves information on credential revocation.
    *
    * @param batchId
    *   batch containing the credential.
    * @param credentialHash
    *   hash represents the credential inside the batch.
    * @return
    */
  def getCredentialRevocationData(
      batchId: String,
      credentialHash: ByteString
  ): F[Either[NodeError, CredentialRevocationTime]]

  /** Schedules a list of operations for further publication to the underlying ledger.
    *
    * @param ops
    *   one or more operation.
    */
  def scheduleAtalaOperations(ops: node_models.SignedAtalaOperation*): F[List[Either[NodeError, AtalaOperationId]]]

  /** Retrieves a list of scheduled operations.
    *
    * @return
    */
  def getScheduledAtalaOperations: F[Either[NodeError, List[node_models.SignedAtalaOperation]]]

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

  def getWalletTransactions(
      transactionType: node_api.GetWalletTransactionsRequest.TransactionState,
      lastSeenTransactionId: Option[TransactionId],
      limit: Int = 50
  ): F[Either[NodeError, List[TransactionInfo]]]

  def getWalletBalance: F[Either[CardanoWalletError, Balance]]
}

private final class NodeServiceImpl[F[_]: MonadThrow](
    didDataRepository: DIDDataRepository[F],
    underlyingLedger: UnderlyingLedger[F],
    objectManagement: ObjectManagementService[F],
    credentialBatchesRepository: CredentialBatchesRepository[F],
    didPublicKeysLimit: Int
) extends NodeService[F] {
  override def getDidDocumentByDid(didStr: String): F[Either[GettingDidError, DidDocument]] =
    Try(PrismDid.canonicalFromString(didStr)).fold(
      _ => Applicative[F].pure(UnsupportedDidFormat.asLeft[DidDocument]),
      did => getDidDocumentByDid(did)
    )

  private def getDidDocumentByDid(canon: CanonicalPrismDid): F[Either[GettingDidError, DidDocument]] = {
    val getDidResultF: F[Either[GettingDidError, Option[(DIDData, Sha256Digest)]]] =
      didDataRepository
        .findByDid(canon, Some(didPublicKeysLimit))
        .map(_.bimap(GettingCanonicalPrismDidError, toDidDataProto(_, canon)))

    for {
      lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
      getDidResult <- getDidResultF
      res = getDidResult.map(disResult => DidDocument(disResult.map(_._1), disResult.map(_._2), lastSyncedTimestamp))
    } yield res
  }

  private def toDidDataProto(in: Option[DIDDataState], canon: CanonicalPrismDid): Option[(DIDData, Sha256Digest)] =
    in.map(didDataState => (ProtoCodecs.toDIDDataProto(canon.getSuffix, didDataState), didDataState.lastOperation))

  override def getBatchState(batchIdStr: String): F[Either[NodeError, BatchData]] = {
    // NOTE: CredentialBatchId.fromString returns null and doesn't throw an error when string wasn't successfully parsed
    Option(CredentialBatchId.fromString(batchIdStr))
      .fold(
        Applicative[F].pure((NodeError.InvalidArgument(s"Invalid batch id: $batchIdStr"): NodeError).asLeft[BatchData])
      )(batchId => getBatchState(batchId))
  }

  private def getBatchState(batchId: CredentialBatchId): F[Either[NodeError, BatchData]] =
    for {
      lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
      maybeBatchStateE <- credentialBatchesRepository.getBatchState(batchId)
      batchData = maybeBatchStateE.map(BatchData(_, lastSyncedTimestamp))
    } yield batchData

  override def getCredentialRevocationData(
      batchIdStr: String,
      credentialHashBS: ByteString
  ): F[Either[NodeError, CredentialRevocationTime]] = {
    // NOTE: CredentialBatchId.fromString returns null and doesn't throw an error when string wasn't successfully parsed
    Option(CredentialBatchId.fromString(batchIdStr))
      .fold(
        Applicative[F].pure(
          (NodeError.InvalidArgument(s"Invalid batch id: $batchIdStr"): NodeError).asLeft[CredentialRevocationTime]
        )
      )(batchId =>
        Try(Sha256Digest.fromBytes(credentialHashBS.toByteArray)).fold(
          _ =>
            Applicative[F].pure(
              NodeError
                .InvalidArgument(
                  s"The given byte array does not correspond to a SHA256 hash. It must have exactly 32 bytes: ${credentialHashBS.toByteArray.map("%02X" format _).mkString}"
                )
                .asLeft
            ),
          credentialHash => getCredentialRevocationData(batchId, credentialHash)
        )
      )
  }

  private def getCredentialRevocationData(
      batchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): F[Either[NodeError, CredentialRevocationTime]] =
    for {
      lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
      maybeTime <- credentialBatchesRepository.getCredentialRevocationTime(batchId, credentialHash)
      credentialRevocationTime = maybeTime.map(CredentialRevocationTime(_, lastSyncedTimestamp))
    } yield credentialRevocationTime

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

  override def getScheduledAtalaOperations: F[Either[NodeError, List[SignedAtalaOperation]]] =
    objectManagement.getScheduledAtalaObjects
      .map(_.map(_.flatMap(obj => obj.getAtalaBlock.map(_.operations).getOrElse(Seq()))))

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

  override def getWalletTransactions(
      transactionType: GetWalletTransactionsRequest.TransactionState,
      lastSeenTransactionId: Option[TransactionId],
      limit: Int
  ): F[Either[NodeError, List[TransactionInfo]]] = {
    transactionType match {
      case GetWalletTransactionsRequest.TransactionState.Ongoing =>
        objectManagement.getUnconfirmedTransactions(lastSeenTransactionId, limit)
      case GetWalletTransactionsRequest.TransactionState.Confirmed =>
        objectManagement.getConfirmedTransactions(lastSeenTransactionId, limit)
      case _ =>
        Either
          .left[NodeError, List[TransactionInfo]](InvalidArgument("Unrecognized transaction type"): NodeError)
          .pure[F]
    }
  }

  override def getWalletBalance: F[Either[CardanoWalletError, Balance]] =
    underlyingLedger.getWalletBalance
}

object NodeService {

  def make[I[_]: Functor, F[_]: MonadThrow](
      didDataRepository: DIDDataRepository[F],
      underlyingLedger: UnderlyingLedger[F],
      objectManagement: ObjectManagementService[F],
      credentialBatchesRepository: CredentialBatchesRepository[F],
      didPublicKeysLimit: Int,
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
        underlyingLedger,
        objectManagement,
        credentialBatchesRepository,
        didPublicKeysLimit
      )
    }
  }

  def resource[I[_]: Comonad, F[_]: MonadThrow](
      didDataRepository: DIDDataRepository[F],
      underlyingLedger: UnderlyingLedger[F],
      objectManagement: ObjectManagementService[F],
      credentialBatchesRepository: CredentialBatchesRepository[F],
      didPublicKeysLimit: Int,
      logs: Logs[I, F]
  ): Resource[I, NodeService[F]] = Resource.eval(
    make(didDataRepository, underlyingLedger, objectManagement, credentialBatchesRepository, didPublicKeysLimit, logs)
  )

  def unsafe[I[_]: Comonad, F[_]: MonadThrow](
      didDataRepository: DIDDataRepository[F],
      underlyingLedger: UnderlyingLedger[F],
      objectManagement: ObjectManagementService[F],
      credentialBatchesRepository: CredentialBatchesRepository[F],
      didPublicKeysLimit: Int,
      logs: Logs[I, F]
  ): NodeService[F] =
    make(
      didDataRepository,
      underlyingLedger,
      objectManagement,
      credentialBatchesRepository,
      didPublicKeysLimit,
      logs
    ).extract

}

final case class BatchData(maybeBatchState: Option[CredentialBatchState], lastSyncedTimestamp: Instant)

final case class CredentialRevocationTime(maybeLedgerData: Option[LedgerData], lastSyncedTimestamp: Instant)

final case class DidDocument(
    maybeData: Option[DIDData],
    maybeOperation: Option[Sha256Digest],
    lastSyncedTimeStamp: Instant
)

@derive(loggable)
final case class OperationInfo(maybeOperationInfo: Option[AtalaOperationInfo], lastSyncedTimestamp: Instant)

@derive(loggable)
sealed trait GettingDidError

final case class GettingCanonicalPrismDidError(node: NodeError) extends GettingDidError

case object UnsupportedDidFormat extends GettingDidError
