package io.iohk.atala.prism.console.services

import cats.implicits.catsSyntaxApplicativeId
import cats.syntax.either._
import cats.syntax.functor._
import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.{ConnectorError, ConnectorErrorSupport, InternalServerError, InvalidRequest}
import io.iohk.atala.prism.console.grpc.ProtoCodecs._
import io.iohk.atala.prism.console.grpc._
import io.iohk.atala.prism.console.integrations.CredentialsIntegrationService
import io.iohk.atala.prism.console.models._
import io.iohk.atala.prism.console.models.actions.{
  CreateGenericCredentialRequest,
  GetBlockchainDataRequest,
  GetContactCredentialsRequest,
  GetGenericCredentialsRequest,
  PublishBatchRequest,
  ShareCredentialRequest,
  StorePublishedCredentialRequest
}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, CredentialsRepository}
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{ParticipantId, TransactionId, TransactionInfo, ProtoCodecs => CommonProtoCodecs}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{common_models, console_api, node_api}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.{FutureEitherFOps, FutureEitherOps, FutureOptionOps}
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class CredentialsServiceImpl(
    credentialsRepository: CredentialsRepository,
    contactsRepository: ContactsRepository,
    credentialsIntegration: CredentialsIntegrationService,
    protected val authenticator: ConnectorAuthenticator,
    nodeService: NodeServiceGrpc.NodeService
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsServiceGrpc.CredentialsService
    with ConnectorErrorSupport
    with AuthSupport[ConnectorError, ParticipantId] {

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createGenericCredential(
      request: console_api.CreateGenericCredentialRequest
  ): Future[console_api.CreateGenericCredentialResponse] =
    auth[CreateGenericCredentialRequest]("createGenericCredential", request) { (participantId, createCredRequest) =>
      val institutionId = Institution.Id(participantId.uuid)
      // get the subjectId from the externalId
      // TODO: Avoid doing this when we stop accepting the subjectId
      val subjectIdF = createCredRequest.maybeExternalId.fold(
        createCredRequest.maybeContactId
          .pure[Future]
          .toFutureEither(InvalidRequest("The contactId is required, if it was provided, it's an invalid value"))
      )(externalId =>
        contactsRepository
          .find(institutionId, externalId)
          .flatMap(_.pure[Future].toFutureEither(InvalidRequest("The given externalId doesn't exists")))
          .map(_.contactId)
      )

      for {
        contactId <- subjectIdF
        model =
          request
            .into[CreateGenericCredential]
            .withFieldConst(_.issuedBy, institutionId)
            .withFieldConst(_.subjectId, contactId)
            .withFieldConst(_.credentialData, createCredRequest.credentialData)
            .enableUnsafeOption
            .transform
        created <-
          credentialsRepository
            .create(model)
            .map(genericCredentialToProto)
      } yield console_api.CreateGenericCredentialResponse().withGenericCredential(created)
    }

  override def getGenericCredentials(
      request: console_api.GetGenericCredentialsRequest
  ): Future[console_api.GetGenericCredentialsResponse] =
    auth[GetGenericCredentialsRequest]("getGenericCredentials", request) { (issuerId, getGenericCredsRequest) =>
      val institutionId = Institution.Id(issuerId.uuid)
      credentialsRepository
        .getBy(institutionId, getGenericCredsRequest.limit, getGenericCredsRequest.offset)
        .map { list =>
          console_api.GetGenericCredentialsResponse(list.map(genericCredentialToProto))
        }
    }

  private def extractValues(signedAtalaOperation: SignedAtalaOperation): (MerkleRoot, DID, SHA256Digest) = {
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

  override def getContactCredentials(
      request: console_api.GetContactCredentialsRequest
  ): Future[console_api.GetContactCredentialsResponse] =
    auth[GetContactCredentialsRequest]("getSubjectCredentials", request) { (participantId, getContactCredsRequest) =>
      val institutionId = Institution.Id(participantId.uuid)
      credentialsRepository
        .getBy(institutionId, getContactCredsRequest.contactId)
        .map { list =>
          console_api.GetContactCredentialsResponse(list.map(genericCredentialToProto))
        }
    }

  override def shareCredential(
      request: console_api.ShareCredentialRequest
  ): Future[console_api.ShareCredentialResponse] =
    auth[ShareCredentialRequest]("shareCredential", request) { (participantId, shareCredentialRequest) =>
      val institutionId = Institution.Id(participantId.uuid)
      credentialsRepository
        .markAsShared(institutionId, shareCredentialRequest.cmanagerCredentialId)
        .as(console_api.ShareCredentialResponse())
    }

  /** Retrieves node information associated to a credential
    */
  override def getBlockchainData(
      request: console_api.GetBlockchainDataRequest
  ): Future[console_api.GetBlockchainDataResponse] =
    auth[GetBlockchainDataRequest]("getBlockchainData", request) { (_, getBlockchainDataRequest) =>
      val result = for {
        response <-
          nodeService
            .getBatchState(
              node_api
                .GetBatchStateRequest()
                .withBatchId(getBlockchainDataRequest.batchId.id)
            )
      } yield response.publicationLedgerData.fold(console_api.GetBlockchainDataResponse())(ledgerData =>
        console_api
          .GetBlockchainDataResponse()
          .withIssuanceProof(
            common_models
              .TransactionInfo()
              .withTransactionId(ledgerData.transactionId)
              .withLedger(ledgerData.ledger)
          )
      )
      result.lift
    }

  /** Publishes a credential batch to the blockchain.
    * This method stores the published credentials on the database, and invokes the necessary
    * methods to get it published to the blockchain.
    */
  override def publishBatch(request: console_api.PublishBatchRequest): Future[console_api.PublishBatchResponse] = {
    def storeBatch(
        batchId: CredentialBatchId,
        signedIssueCredentialBatchOp: SignedAtalaOperation,
        transactionInfo: common_models.TransactionInfo
    ): FutureEither[ConnectorError, Unit] = {
      for {
        ledger <- CommonProtoCodecs.fromLedger(transactionInfo.ledger).asRight.pure[Future].toFutureEither
        transactionId <-
          TransactionId
            .from(transactionInfo.transactionId)
            .pure[Future]
            .toFutureEither(InternalServerError(new RuntimeException("Corrupted transaction ID")))
        (merkleRoot, did, operationHash) = extractValues(signedIssueCredentialBatchOp)
        computedBatchId = CredentialBatchId.fromBatchData(did.suffix, merkleRoot)
        // validation for sanity check
        // The `batchId` parameter is the id returned by the node.
        // We make this check to be sure that the node and the console are
        // using the same id (if this fails, they are using different versions
        // of the protocol)
        _ <-
          if (batchId == computedBatchId) ().asRight[ConnectorError].pure[Future].toFutureEither
          else
            InvalidRequest("The batch id provided by the node does not match the one computed")
              .asLeft[Unit]
              .pure[Future]
              .toFutureEither
        _ <-
          credentialsRepository
            .storeBatchData(
              StoreBatchData(
                batchId = batchId,
                issuanceOperationHash = operationHash,
                issuanceTransactionInfo = TransactionInfo(
                  transactionId = transactionId,
                  ledger = ledger,
                  block = None
                )
              )
            )
      } yield ()
    }

    auth[PublishBatchRequest]("publishBatch", request) { (_, publishBatchRequest) =>
      for {
        // Issue the batch in the Node
        credentialIssued <- nodeService.issueCredentialBatch {
          node_api
            .IssueCredentialBatchRequest()
            .withSignedOperation(publishBatchRequest.signedIssueCredentialBatchOp)
        }.lift
        returnedBatchId <-
          CredentialBatchId
            .fromString(credentialIssued.batchId)
            .pure[Future]
            .toFutureEither(InternalServerError(new RuntimeException("Node returned an invalid batch id")))
        transactionInfo <-
          credentialIssued.transactionInfo
            .pure[Future]
            .toFutureEither(InternalServerError(new RuntimeException("We could not generate a transaction")))
        // Update the database
        _ <- storeBatch(returnedBatchId, publishBatchRequest.signedIssueCredentialBatchOp, transactionInfo)
      } yield console_api
        .PublishBatchResponse()
        .withTransactionInfo(transactionInfo)
        .withBatchId(credentialIssued.batchId)
    }
  }

  // This request stores in the console database the information associated to a credential. The endpoint assumes that
  // the credential has been publish in a batch though the PublishBatch endpoint
  override def storePublishedCredential(
      request: console_api.StorePublishedCredentialRequest
  ): Future[console_api.StorePublishedCredentialResponse] =
    auth[StorePublishedCredentialRequest]("storePublishedCredential", request) { (participantId, storeCredsRequest) =>
      for {
        maybeCredential <- credentialsRepository.getBy(storeCredsRequest.consoleCredentialId)
        credential <-
          maybeCredential
            .pure[Future]
            .toFutureEither(
              InvalidRequest(s"Credential with ID ${storeCredsRequest.consoleCredentialId} does not exist")
            )
        issuerId = Institution.Id(participantId.uuid)
        // Verify issuer
        _ <-
          if (credential.issuedBy == issuerId) FutureEither.right(())
          else FutureEither.left(InvalidRequest("The credential was not issued by the specified issuer"))
        _ <-
          credentialsRepository
            .storeCredentialPublicationData(
              issuerId,
              CredentialPublicationData(
                consoleCredentialId = storeCredsRequest.consoleCredentialId,
                credentialBatchId = storeCredsRequest.batchId,
                encodedSignedCredential = storeCredsRequest.encodedSignedCredential,
                proofOfInclusion = storeCredsRequest.encodedInclusionProof
              )
            )
      } yield console_api.StorePublishedCredentialResponse()
    }

  override def revokePublishedCredential(
      request: console_api.RevokePublishedCredentialRequest
  ): Future[console_api.RevokePublishedCredentialResponse] = {
    auth[RevokePublishedCredential]("revokePublishedCredential", request) { (participantId, query) =>
      credentialsIntegration
        .revokePublishedCredential(Institution.Id(participantId.uuid), query)
        .map { info =>
          console_api
            .RevokePublishedCredentialResponse()
            .withTransactionInfo(info)
        }
    }
  }

  // Do not implement, this is implemented in the management console
  override def deleteCredentials(
      request: console_api.DeleteCredentialsRequest
  ): Future[console_api.DeleteCredentialsResponse] = ???

  // Do not implement, this is implemented in the management console
  override def getLedgerData(request: console_api.GetLedgerDataRequest): Future[console_api.GetLedgerDataResponse] = ???

  // Do not implement, this is implemented in the management console
  override def shareCredentials(
      request: console_api.ShareCredentialsRequest
  ): Future[console_api.ShareCredentialsResponse] = ???
}
