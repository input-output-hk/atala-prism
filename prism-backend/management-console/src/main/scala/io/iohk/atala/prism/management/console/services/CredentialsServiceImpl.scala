package io.iohk.atala.prism.management.console.services

import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.either._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.AuthAndMiddlewareSupport
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.errors.{
  InternalServerError,
  ManagementConsoleError,
  ManagementConsoleErrorSupport
}
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.genericCredentialToProto
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.integrations.CredentialsIntegrationService
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialsRepository
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.node_api.IssueCredentialBatchResponse
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.utils.FutureEither.FutureEitherFOps
import org.slf4j.{Logger, LoggerFactory}
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps

import scala.concurrent.{ExecutionContext, Future}

import io.iohk.atala.prism.interop.toKotlinSDK._
import io.iohk.atala.prism.interop.toScalaSDK._

class CredentialsServiceImpl(
    credentialsRepository: CredentialsRepository[IO],
    credentialsIntegrationService: CredentialsIntegrationService,
    val authenticator: ManagementConsoleAuthenticator,
    nodeService: NodeServiceGrpc.NodeService,
    connectorClient: ConnectorClient
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsServiceGrpc.CredentialsService
    with ManagementConsoleErrorSupport
    with AuthAndMiddlewareSupport[ManagementConsoleError, ParticipantId] {

  override protected val serviceName: String = "credentials-service"

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGenericCredential(
      request: CreateGenericCredentialRequest
  ): Future[CreateGenericCredentialResponse] =
    auth[CreateGenericCredential]("createGenericCredential", request) { (participantId, query) =>
      credentialsIntegrationService
        .createGenericCredential(participantId, query)
        .toFutureEither
        .map { genericCredentialWithConnection =>
          genericCredentialToProto(
            genericCredentialWithConnection.genericCredential,
            genericCredentialWithConnection.connection
          )
        }
        .map { created =>
          console_api.CreateGenericCredentialResponse().withGenericCredential(created)
        }
    }

  override def getGenericCredentials(
      request: GetGenericCredentialsRequest
  ): Future[GetGenericCredentialsResponse] =
    auth[GenericCredential.PaginatedQuery]("getGenericCredentials", request) { (participantId, query) =>
      credentialsIntegrationService
        .getGenericCredentials(participantId, query)
        .toFutureEither
        .map { result =>
          console_api.GetGenericCredentialsResponse(
            result.data.map(genericCredentialsResult =>
              genericCredentialToProto(genericCredentialsResult.genericCredential, genericCredentialsResult.connection)
            )
          )
        }
    }

  override def getContactCredentials(request: GetContactCredentialsRequest): Future[GetContactCredentialsResponse] =
    auth[GetContactCredentials]("getContactCredentials", request) { (participantId, query) =>
      credentialsIntegrationService
        .getContactCredentials(participantId, query.contactId)
        .toFutureEither
        .map { result =>
          console_api.GetContactCredentialsResponse(
            result.data.map(genericCredentialsResult =>
              genericCredentialToProto(genericCredentialsResult.genericCredential, genericCredentialsResult.connection)
            )
          )
        }
    }

  override def shareCredential(request: ShareCredentialRequest): Future[ShareCredentialResponse] =
    auth[ShareCredential]("shareCredential", request) { (participantId, query) =>
      credentialsRepository
        .markAsShared(participantId, NonEmptyList.of(query.credentialId))
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
        .map { _ =>
          console_api.ShareCredentialResponse()
        }
    }

  override def getBlockchainData(request: GetBlockchainDataRequest): Future[GetBlockchainDataResponse] = ???

  override def publishBatch(request: PublishBatchRequest): Future[PublishBatchResponse] = {
    def extractValues(
        signedAtalaOperation: SignedAtalaOperation
    ): FutureEither[ManagementConsoleError, (MerkleRoot, DID, SHA256Digest)] = {
      val maybePair = for {
        atalaOperation <- signedAtalaOperation.operation
        opHash = SHA256Digest.compute(atalaOperation.toByteArray)
        issueCredentialBatch <- atalaOperation.operation.issueCredentialBatch
        credentialBatchData <- issueCredentialBatch.credentialBatchData
        did = DID.buildPrismDID(credentialBatchData.issuerDid)
        merkleRoot = new MerkleRoot(SHA256Digest.fromBytes(credentialBatchData.merkleRoot.toByteArray))
      } yield (merkleRoot, did, opHash)
      maybePair.fold(
        InternalServerError(new RuntimeException("Failed to extract content hash and issuer DID"))
          .asLeft[(MerkleRoot, DID, SHA256Digest)]
          .pure[Future]
          .toFutureEither
      )(_.asRight.pure[Future].toFutureEither)
    }

    def storeBatch(
        batchId: CredentialBatchId,
        signedIssueCredentialBatchOp: SignedAtalaOperation
    ): FutureEither[ManagementConsoleError, Unit] = {
      for {
        value <- extractValues(signedIssueCredentialBatchOp)
        (merkleRoot, did, operationHash) = value
        computedBatchId = CredentialBatchId.fromBatchData(did.suffix, merkleRoot.asScala)
        // validation for sanity check
        // The `batchId` parameter is the id returned by the node.
        // We make this check to be sure that the node and the console are
        // using the same id (if this fails, they are using different versions
        // of the protocol)
        _ = if (batchId != computedBatchId)
          logger.warn("The batch id provided by the node does not match the one computed")
        _ <-
          credentialsRepository
            .storeBatchData(
              batchId = batchId,
              issuanceOperationHash = operationHash,
              AtalaOperationId.of(signedIssueCredentialBatchOp)
            )
            .unsafeToFuture()
            .map(_.asRight)
            .toFutureEither
      } yield ()
    }

    auth[PublishBatch]("publishBatch", request) { (_, query) =>
      for {
        response <-
          nodeService
            .issueCredentialBatch(
              node_api
                .IssueCredentialBatchRequest()
                .withSignedOperation(query.signedOperation)
            )
            .map(ProtoConverter[IssueCredentialBatchResponse, IssueCredentialBatchNodeResponse].fromProto)
            .map(_.toEither)
            .toFutureEither(ex => wrapAsServerError(ex))
        _ <- storeBatch(response.batchId, query.signedOperation)
      } yield PublishBatchResponse()
        .withBatchId(response.batchId.getId)
        .withOperationId(response.operationId.toProtoByteString)
    }
  }

  override def revokePublishedCredential(
      request: RevokePublishedCredentialRequest
  ): Future[RevokePublishedCredentialResponse] = {
    auth[RevokePublishedCredential]("revokePublishedCredential", request) { (participantId, query) =>
      credentialsIntegrationService
        .revokePublishedCredential(participantId, query)
        .map { operationId =>
          RevokePublishedCredentialResponse().withOperationId(operationId.toProtoByteString)
        }
    }
  }

  override def deleteCredentials(request: DeleteCredentialsRequest): Future[DeleteCredentialsResponse] = {
    auth[DeleteCredentials]("deleteCredentials", request) { (participantId, query) =>
      credentialsRepository
        .deleteCredentials(participantId, query.credentialsIds)
        .unsafeToFuture()
        .toFutureEither
        .as(DeleteCredentialsResponse())
    }
  }

  override def storePublishedCredential(
      request: StorePublishedCredentialRequest
  ): Future[StorePublishedCredentialResponse] = {
    auth[StorePublishedCredential]("storePublishedCredential", request) { (participantId, query) =>
      val result = for {
        maybeCredential <- credentialsRepository.getBy(query.consoleCredentialId)
        credential = maybeCredential.getOrElse(
          throw new RuntimeException(s"Credential with ID ${query.consoleCredentialId} does not exist")
        )
        // Verify issuer
        _ = require(credential.issuedBy == participantId, "The credential was not issued by the specified issuer")
        _ <- credentialsRepository.storePublicationData(
          issuerId = participantId,
          credentialData = PublishCredential(
            query.consoleCredentialId,
            query.batchId,
            query.encodedSignedCredential,
            query.inclusionProof
          )
        )
      } yield StorePublishedCredentialResponse()
      result
        .unsafeToFuture()
        .map(_.asRight)
        .toFutureEither
    }
  }

  override def getLedgerData(request: GetLedgerDataRequest): Future[GetLedgerDataResponse] = {
    auth[GetLedgerData]("getLedgerData", request) { (_, query) =>
      val result = for {
        batchState <- nodeService.getBatchState(
          node_api
            .GetBatchStateRequest()
            .withBatchId(query.batchId.getId)
        )
        credentialLedgerData <- nodeService.getCredentialRevocationTime(
          node_api
            .GetCredentialRevocationTimeRequest()
            .withBatchId(query.batchId.id)
            .withCredentialHash(ByteString.copyFrom(query.credentialHash.getValue))
        )
      } yield GetLedgerDataResponse(
        batchIssuance = batchState.publicationLedgerData,
        batchRevocation = batchState.revocationLedgerData,
        credentialRevocation = credentialLedgerData.revocationLedgerData
      )

      result.lift
    }
  }

  override def shareCredentials(
      request: console_api.ShareCredentialsRequest
  ): Future[console_api.ShareCredentialsResponse] = {
    auth[ShareCredentials]("shareCredentials", request) { (participantId, query) =>
      for {
        _ <-
          credentialsRepository
            .verifyPublishedCredentialsExist(participantId, query.credentialsIds)
            .unsafeToFuture()
            .toFutureEither
        _ <-
          connectorClient
            .sendMessages(
              query.sendMessagesRequest,
              query.sendMessagesRequestMetadata
            )
            .lift

        _ <-
          credentialsRepository
            .markAsShared(participantId, query.credentialsIds)
            .unsafeToFuture()
            .map(_.asRight)
            .toFutureEither
      } yield ShareCredentialsResponse()
    }
  }

}
