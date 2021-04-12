package io.iohk.atala.prism.management.console.services

import cats.data.NonEmptyList
import cats.syntax.functor._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.errors.{ManagementConsoleError, ManagementConsoleErrorSupport}
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.genericCredentialToProto
import io.iohk.atala.prism.management.console.grpc._
import io.iohk.atala.prism.management.console.integrations.CredentialsIntegrationService
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialsRepository
import io.iohk.atala.prism.models.{TransactionInfo, ProtoCodecs => CommonProtoCodecs}
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.common_models
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
import scala.util.Try

class CredentialsServiceImpl(
    credentialsRepository: CredentialsRepository,
    credentialsIntegrationService: CredentialsIntegrationService,
    val authenticator: ManagementConsoleAuthenticator,
    nodeService: NodeServiceGrpc.NodeService,
    connectorClient: ConnectorClient
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsServiceGrpc.CredentialsService
    with ManagementConsoleErrorSupport
    with AuthSupport[ManagementConsoleError, ParticipantId] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGenericCredential(
      request: CreateGenericCredentialRequest
  ): Future[CreateGenericCredentialResponse] =
    auth[CreateGenericCredential]("createGenericCredential", request) { (participantId, query) =>
      credentialsRepository
        .create(participantId, query)
        .map(genericCredentialToProto)
        .map { created =>
          console_api.CreateGenericCredentialResponse().withGenericCredential(created)
        }
    }

  override def getGenericCredentials(
      request: GetGenericCredentialsRequest
  ): Future[GetGenericCredentialsResponse] =
    auth[GetGenericCredential]("getGenericCredentials", request) { (participantId, query) =>
      credentialsRepository
        .getBy(participantId, query.limit, query.lastSeenCredentialId)
        .map { list =>
          console_api.GetGenericCredentialsResponse(list.map(genericCredentialToProto))
        }
    }

  override def getContactCredentials(request: GetContactCredentialsRequest): Future[GetContactCredentialsResponse] =
    auth[GetContactCredentials]("getContactCredentials", request) { (participantId, query) =>
      credentialsRepository
        .getBy(participantId, query.contactId)
        .map { list =>
          console_api.GetContactCredentialsResponse(list.map(genericCredentialToProto))
        }
    }

  override def shareCredential(request: ShareCredentialRequest): Future[ShareCredentialResponse] =
    auth[ShareCredential]("shareCredential", request) { (participantId, query) =>
      credentialsRepository
        .markAsShared(participantId, NonEmptyList.of(query.credentialId))
        .map { _ =>
          console_api.ShareCredentialResponse()
        }
    }

  override def getBlockchainData(request: GetBlockchainDataRequest): Future[GetBlockchainDataResponse] = ???

  override def publishBatch(request: PublishBatchRequest): Future[PublishBatchResponse] = {
    def extractValues(signedAtalaOperation: SignedAtalaOperation): (MerkleRoot, DID, SHA256Digest) = {
      val maybePair = for {
        atalaOperation <- signedAtalaOperation.operation
        opHash = SHA256Digest.compute(atalaOperation.toByteArray)
        issueCredentialBatch <- atalaOperation.operation.issueCredentialBatch
        credentialBatchData <- issueCredentialBatch.credentialBatchData
        did = DID.buildPrismDID(credentialBatchData.issuerDID)
        merkleRoot = MerkleRoot(SHA256Digest.fromVectorUnsafe(credentialBatchData.merkleRoot.toByteArray.toVector))
      } yield (merkleRoot, did, opHash)
      maybePair.getOrElse(throw new RuntimeException("Failed to extract content hash and issuer DID"))
    }

    def storeBatch(
        batchId: CredentialBatchId,
        signedIssueCredentialBatchOp: SignedAtalaOperation,
        transactionInfo: common_models.TransactionInfo
    ): FutureEither[ManagementConsoleError, Unit] = {
      for {
        validatedTransactionInfo <-
          Future
            .successful(
              Try(CommonProtoCodecs.fromTransactionInfo(transactionInfo)).toEither
            )
            .toFutureEither(ex => wrapAsServerError(ex))
        ledger = validatedTransactionInfo.ledger
        transactionId = validatedTransactionInfo.transactionId
        (merkleRoot, did, operationHash) = extractValues(signedIssueCredentialBatchOp)
        computedBatchId = CredentialBatchId.fromBatchData(did.suffix, merkleRoot)
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
              issuanceTransactionInfo = TransactionInfo(
                transactionId = transactionId,
                ledger = ledger,
                block = None
              )
            )
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
        _ <- storeBatch(response.batchId, query.signedOperation, response.transactionInfo)
      } yield PublishBatchResponse()
        .withBatchId(response.batchId.id)
        .withTransactionInfo(response.transactionInfo)
    }
  }

  override def revokePublishedCredential(
      request: RevokePublishedCredentialRequest
  ): Future[RevokePublishedCredentialResponse] = {
    auth[RevokePublishedCredential]("revokePublishedCredential", request) { (participantId, query) =>
      credentialsIntegrationService
        .revokePublishedCredential(participantId, query)
        .map(RevokePublishedCredentialResponse().withTransactionInfo)
    }
  }

  override def deleteCredentials(request: DeleteCredentialsRequest): Future[DeleteCredentialsResponse] = {
    auth[DeleteCredentials]("deleteCredentials", request) { (participantId, query) =>
      credentialsRepository
        .deleteCredentials(participantId, query.credentialsIds)
        .as(DeleteCredentialsResponse())
    }
  }

  override def storePublishedCredential(
      request: StorePublishedCredentialRequest
  ): Future[StorePublishedCredentialResponse] = {
    auth[StorePublishedCredential]("storePublishedCredential", request) { (participantId, query) =>
      for {
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
    }
  }

  override def getLedgerData(request: GetLedgerDataRequest): Future[GetLedgerDataResponse] = {
    auth[GetLedgerData]("getLedgerData", request) { (_, query) =>
      val result = for {
        batchState <- nodeService.getBatchState(
          node_api
            .GetBatchStateRequest()
            .withBatchId(query.batchId.id)
        )
        credentialLedgerData <- nodeService.getCredentialRevocationTime(
          node_api
            .GetCredentialRevocationTimeRequest()
            .withBatchId(query.batchId.id)
            .withCredentialHash(ByteString.copyFrom(query.credentialHash.value.toArray))
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
        _ <- credentialsRepository.verifyPublishedCredentialsExist(participantId, query.credentialsIds)
        _ <-
          connectorClient
            .sendMessages(
              query.sendMessagesRequest,
              query.sendMessagesRequestMetadata
            )
            .lift

        _ <- credentialsRepository.markAsShared(participantId, query.credentialsIds)
      } yield ShareCredentialsResponse()
    }
  }

}
