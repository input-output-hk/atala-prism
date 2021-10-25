package io.iohk.atala.prism.management.console.services

import cats.{Comonad, Functor, Monad}
import cats.implicits.toFunctorOps
import cats.data.NonEmptyList
import cats.effect.{MonadThrow, Resource}
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.comonad._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.traverse._
import com.google.protobuf.ByteString
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256, Sha256Digest}
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.errors
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.errors.{
  InternalServerError,
  ManagementConsoleError,
  ManagementConsoleErrorSupport
}
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.integrations.CredentialsIntegrationService
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialsRepository
import io.iohk.atala.prism.protos.node_api.{IssueCredentialBatchResponse, NodeServiceGrpc}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.node_api
import org.slf4j.{Logger, LoggerFactory}
import io.iohk.atala.prism.management.console.integrations.CredentialsIntegrationService.{
  GenericCredentialWithConnection,
  GetGenericCredentialsResult
}
import io.iohk.atala.prism.management.console.models.GenericCredential.PaginatedQuery
import io.iohk.atala.prism.logging.GeneralLoggableInstances._
import io.iohk.atala.prism.models.DidSuffix
import tofu.Execute
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._

@derive(applyK)
trait CredentialsService[F[_]] {
  def createGenericCredential(
      participantId: ParticipantId,
      createGenericCredential: CreateGenericCredential
  ): F[Either[errors.ManagementConsoleError, GenericCredentialWithConnection]]

  def getGenericCredentials(
      participantId: ParticipantId,
      query: GenericCredential.PaginatedQuery
  ): F[GetGenericCredentialsResult]

  def getContactCredentials(
      participantId: ParticipantId,
      getContactCredentials: GetContactCredentials
  ): F[GetGenericCredentialsResult]

  def shareCredential(
      participantId: ParticipantId,
      credId: NonEmptyList[GenericCredential.Id]
  ): F[Unit]

  def publishBatch(
      publishBatch: PublishBatch
  ): F[Either[ManagementConsoleError, IssueCredentialBatchNodeResponse]]

  def revokePublishedCredential(
      participantId: ParticipantId,
      revokePublishedCredential: RevokePublishedCredential
  ): F[Either[ManagementConsoleError, AtalaOperationId]]

  def deleteCredentials(
      participantId: ParticipantId,
      deleteCredentials: DeleteCredentials
  ): F[Either[ManagementConsoleError, Unit]]

  def storePublishedCredential(
      participantId: ParticipantId,
      storePublishedCredential: StorePublishedCredential
  ): F[Int]

  def getLedgerData(getLedgerData: GetLedgerData): F[GetLedgerDataResult]

  def shareCredentials(
      participantId: ParticipantId,
      shareCredentials: ShareCredentials
  ): F[Either[ManagementConsoleError, Unit]]

}

object CredentialsService {
  def apply[F[_]: Execute: MonadThrow, R[_]: Functor](
      credentialsRepository: CredentialsRepository[F],
      credentialsIntegrationService: CredentialsIntegrationService[F],
      nodeService: NodeServiceGrpc.NodeService,
      connectorClient: ConnectorClient[F],
      logs: Logs[R, F]
  ): R[CredentialsService[F]] =
    for {
      serviceLogs <- logs.service[CredentialsService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialsService[F]] =
        serviceLogs
      val logs: CredentialsService[Mid[F, *]] = new CredentialsServiceLogs[F]
      val mid = logs
      mid attach new CredentialsServiceImpl[F](
        credentialsRepository,
        credentialsIntegrationService,
        nodeService,
        connectorClient
      )
    }

  def unsafe[F[_]: Execute: MonadThrow, R[_]: Comonad](
      credentialsRepository: CredentialsRepository[F],
      credentialsIntegrationService: CredentialsIntegrationService[F],
      nodeService: NodeServiceGrpc.NodeService,
      connectorClient: ConnectorClient[F],
      logs: Logs[R, F]
  ): CredentialsService[F] =
    CredentialsService(
      credentialsRepository,
      credentialsIntegrationService,
      nodeService,
      connectorClient,
      logs
    ).extract

  def makeResource[F[_]: Execute: MonadThrow, R[_]: Monad](
      credentialsRepository: CredentialsRepository[F],
      credentialsIntegrationService: CredentialsIntegrationService[F],
      nodeService: NodeServiceGrpc.NodeService,
      connectorClient: ConnectorClient[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialsService[F]] =
    Resource.eval(
      CredentialsService(
        credentialsRepository,
        credentialsIntegrationService,
        nodeService,
        connectorClient,
        logs
      )
    )
}

private final class CredentialsServiceImpl[F[_]: Monad](
    credentialsRepository: CredentialsRepository[F],
    credentialsIntegrationService: CredentialsIntegrationService[F],
    nodeService: NodeServiceGrpc.NodeService,
    connectorClient: ConnectorClient[F]
)(implicit ex: Execute[F])
    extends ManagementConsoleErrorSupport
    with CredentialsService[F] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGenericCredential(
      participantId: ParticipantId,
      createGenericCredential: CreateGenericCredential
  ): F[Either[errors.ManagementConsoleError, GenericCredentialWithConnection]] =
    credentialsIntegrationService.createGenericCredential(
      participantId,
      createGenericCredential
    )

  override def getGenericCredentials(
      participantId: ParticipantId,
      query: GenericCredential.PaginatedQuery
  ): F[GetGenericCredentialsResult] =
    credentialsIntegrationService.getGenericCredentials(participantId, query)

  override def getContactCredentials(
      participantId: ParticipantId,
      getContactCredentials: GetContactCredentials
  ): F[GetGenericCredentialsResult] =
    credentialsIntegrationService.getContactCredentials(
      participantId,
      getContactCredentials.contactId
    )

  override def shareCredential(
      participantId: ParticipantId,
      credId: NonEmptyList[GenericCredential.Id]
  ): F[Unit] =
    credentialsRepository.markAsShared(participantId, credId)

  override def publishBatch(
      publishBatch: PublishBatch
  ): F[Either[ManagementConsoleError, IssueCredentialBatchNodeResponse]] = {
    def extractValues(
        signedAtalaOperation: SignedAtalaOperation
    ): Either[ManagementConsoleError, (MerkleRoot, DID, Sha256Digest)] = {
      val maybePair = for {
        atalaOperation <- signedAtalaOperation.operation
        opHash = Sha256.compute(atalaOperation.toByteArray)
        issueCredentialBatch <- atalaOperation.operation.issueCredentialBatch
        credentialBatchData <- issueCredentialBatch.credentialBatchData
        did = DID.fromString(
          DidSuffix.didFromStringSuffix(credentialBatchData.issuerDid)
        )
        merkleRoot = new MerkleRoot(
          Sha256Digest.fromBytes(credentialBatchData.merkleRoot.toByteArray)
        )
      } yield (merkleRoot, did, opHash)
      maybePair.toRight(
        InternalServerError(
          new RuntimeException("Failed to extract content hash and issuer DID")
        )
      )
    }

    def storeBatch(
        batchId: CredentialBatchId,
        signedIssueCredentialBatchOp: SignedAtalaOperation
    ): F[Either[ManagementConsoleError, Int]] = {
      extractValues(signedIssueCredentialBatchOp).traverse { case (merkleRoot, did, operationHash) =>
        val computedBatchId =
          CredentialBatchId.fromBatchData(did.getSuffix, merkleRoot)
        // validation for sanity check
        // The `batchId` parameter is the id returned by the node.
        // We make this check to be sure that the node and the console are
        // using the same id (if this fails, they are using different versions
        // of the protocol)
        if (batchId != computedBatchId)
          logger.warn(
            "The batch id provided by the node does not match the one computed"
          )

        credentialsRepository.storeBatchData(
          batchId = batchId,
          issuanceOperationHash = operationHash,
          AtalaOperationId.of(signedIssueCredentialBatchOp)
        )
      }
    }

    for {
      response <-
        ex.deferFuture(
          nodeService
            .issueCredentialBatch(
              node_api
                .IssueCredentialBatchRequest()
                .withSignedOperation(publishBatch.signedOperation)
            )
        ).map(
          ProtoConverter[
            IssueCredentialBatchResponse,
            IssueCredentialBatchNodeResponse
          ].fromProto
        ).map[Either[ManagementConsoleError, IssueCredentialBatchNodeResponse]](
          _.toEither.left.map(wrapAsServerError)
        )
      result <- response.flatTraverse(response =>
        storeBatch(response.batchId, publishBatch.signedOperation).map(
          _.as(response)
        )
      )
    } yield result
  }

  override def revokePublishedCredential(
      participantId: ParticipantId,
      revokePublishedCredential: RevokePublishedCredential
  ): F[Either[ManagementConsoleError, AtalaOperationId]] =
    credentialsIntegrationService.revokePublishedCredential(
      participantId,
      revokePublishedCredential
    )

  override def deleteCredentials(
      participantId: ParticipantId,
      deleteCredentials: DeleteCredentials
  ): F[Either[ManagementConsoleError, Unit]] =
    credentialsRepository.deleteCredentials(
      participantId,
      deleteCredentials.credentialsIds
    )

  override def storePublishedCredential(
      participantId: ParticipantId,
      storePublishedCredential: StorePublishedCredential
  ): F[Int] =
    for {
      maybeCredential <- credentialsRepository.getBy(
        storePublishedCredential.consoleCredentialId
      )
      credential = maybeCredential.getOrElse(
        throw new RuntimeException(
          s"Credential with ID ${storePublishedCredential.consoleCredentialId} does not exist"
        )
      )
      // Verify issuer
      _ = require(
        credential.issuedBy == participantId,
        "The credential was not issued by the specified issuer"
      )
      storedData <- credentialsRepository.storePublicationData(
        issuerId = participantId,
        credentialData = PublishCredential(
          storePublishedCredential.consoleCredentialId,
          storePublishedCredential.batchId,
          storePublishedCredential.encodedSignedCredential,
          storePublishedCredential.inclusionProof
        )
      )
    } yield storedData

  override def getLedgerData(
      getLedgerData: GetLedgerData
  ): F[GetLedgerDataResult] =
    for {
      batchState <- ex.deferFuture(
        nodeService.getBatchState(
          node_api
            .GetBatchStateRequest()
            .withBatchId(getLedgerData.batchId.getId)
        )
      )
      credentialLedgerData <- ex.deferFuture(
        nodeService.getCredentialRevocationTime(
          node_api
            .GetCredentialRevocationTimeRequest()
            .withBatchId(getLedgerData.batchId.getId)
            .withCredentialHash(
              ByteString.copyFrom(getLedgerData.credentialHash.getValue)
            )
        )
      )
    } yield GetLedgerDataResult(
      publicationData = batchState.publicationLedgerData,
      revocationData = batchState.revocationLedgerData,
      credentialRevocationData = credentialLedgerData.revocationLedgerData
    )

  override def shareCredentials(
      participantId: ParticipantId,
      shareCredentials: ShareCredentials
  ): F[Either[ManagementConsoleError, Unit]] = {
    for {
      verified <- credentialsRepository.verifyPublishedCredentialsExist(
        participantId,
        shareCredentials.credentialsIds
      )
      sentMessages <- verified.traverse(_ =>
        connectorClient.sendMessages(
          shareCredentials.sendMessagesRequest,
          shareCredentials.sendMessagesRequestMetadata
        )
      )
      result <-
        sentMessages.traverse(_ =>
          credentialsRepository.markAsShared(
            participantId,
            shareCredentials.credentialsIds
          )
        )
    } yield result
  }

}

private final class CredentialsServiceLogs[
    F[_]: ServiceLogging[*[_], CredentialsService[F]]: MonadThrow
] extends CredentialsService[Mid[F, *]] {
  override def createGenericCredential(
      participantId: ParticipantId,
      createGenericCredential: CreateGenericCredential
  ): Mid[F, Either[ManagementConsoleError, GenericCredentialWithConnection]] =
    in =>
      info"creating generic credential $participantId" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while creating generic credential $er",
            _ => info"creating generic credential - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while creating generic credential" (_)
        )

  override def getGenericCredentials(
      participantId: ParticipantId,
      query: PaginatedQuery
  ): Mid[F, GetGenericCredentialsResult] =
    in =>
      info"getting generic credentials $participantId" *> in
        .flatTap(_ => info"getting generic credentials - successfully done")
        .onError(
          errorCause"encountered an error while getting generic credentials" (_)
        )

  override def getContactCredentials(
      participantId: ParticipantId,
      getContactCredentials: GetContactCredentials
  ): Mid[F, GetGenericCredentialsResult] =
    in =>
      info"getting contact credentials $participantId" *> in
        .flatTap(_ => info"getting contact credentials - successfully done")
        .onError(
          errorCause"encountered an error while getting contact credentials" (_)
        )

  override def shareCredential(
      participantId: ParticipantId,
      credId: NonEmptyList[GenericCredential.Id]
  ): Mid[F, Unit] =
    in =>
      info"sharing credential $participantId " *>
        in.flatTap(_ => info"sharing credentials - successfully done")
          .onError(
            errorCause"encountered an error while sharing credentials" (_)
          )

  override def publishBatch(
      publishBatch: PublishBatch
  ): Mid[F, Either[ManagementConsoleError, IssueCredentialBatchNodeResponse]] =
    in =>
      info"publishing batch" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while publishing batch $er",
            _ => info"publishing batch - successfully done"
          )
        )
        .onError(errorCause"encountered an error while publishing batch" (_))

  override def revokePublishedCredential(
      participantId: ParticipantId,
      revokePublishedCredential: RevokePublishedCredential
  ): Mid[F, Either[ManagementConsoleError, AtalaOperationId]] =
    in =>
      info"revoking published credential $participantId" *> in
        .flatTap(
          _.fold(
            er => error"encountered an error while revoking published credential $er",
            _ => info"revoking published credential - successfully done"
          )
        )
        .onError(
          errorCause"encountered an error while revoking published credential" (
            _
          )
        )

  override def deleteCredentials(
      participantId: ParticipantId,
      deleteCredentials: DeleteCredentials
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"deleting credentials $participantId" *>
        in.flatTap(
          _.fold(
            e => error"encountered an error while deleting credentials $e",
            _ => info"deleting credentials - successfully done"
          )
        ).onError(
          errorCause"encountered an error while deleting credentials" (_)
        )

  override def storePublishedCredential(
      participantId: ParticipantId,
      storePublishedCredential: StorePublishedCredential
  ): Mid[F, Int] =
    in =>
      info"storing published credential $participantId" *>
        in.flatTap(_ => info"storing published credential - successfully done")
          .onError(
            errorCause"encountered an error while storing published credential" (
              _
            )
          )

  override def getLedgerData(
      getLedgerData: GetLedgerData
  ): Mid[F, GetLedgerDataResult] =
    in =>
      info"getting ledger data ${getLedgerData.batchId}" *>
        in.flatTap(_ => info"getting ledger data - successfully done")
          .onError(
            errorCause"encountered an error while getting ledger data" (_)
          )

  override def shareCredentials(
      participantId: ParticipantId,
      shareCredentials: ShareCredentials
  ): Mid[F, Either[ManagementConsoleError, Unit]] =
    in =>
      info"sharing credentials $participantId" *>
        in.flatTap(
          _.fold(
            e => error"encountered an error while sharing credentials $e",
            _ => info"sharing credentials - successfully done"
          )
        ).onError(
          errorCause"encountered an error while sharing credentials" (_)
        )
}
