package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.auth.AuthSupport
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.errors.{ConnectorError, ConnectorErrorSupport}
import io.iohk.atala.prism.console.grpc.ProtoCodecs._
import io.iohk.atala.prism.console.grpc._
import io.iohk.atala.prism.console.integrations.CredentialsIntegrationService
import io.iohk.atala.prism.console.models._
import io.iohk.atala.prism.console.repositories.{ContactsRepository, CredentialsRepository}
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.{ParticipantId, TransactionId, TransactionInfo, ProtoCodecs => CommonProtoCodecs}
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{common_models, console_api, node_api}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureOptionOps
import io.iohk.atala.prism.utils.syntax._
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
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
      request: CreateGenericCredentialRequest
  ): Future[CreateGenericCredentialResponse] =
    authenticatedHandler("createGenericCredential", request) { issuerId =>
      // get the subjectId from the externalId
      // TODO: Avoid doing this when we stop accepting the subjectId
      val subjectIdF = Option(request.externalId)
        .filter(_.nonEmpty)
        .map(Contact.ExternalId.apply) match {
        case Some(externalId) =>
          contactsRepository
            .find(Institution.Id(issuerId), externalId)
            .map(_.getOrElse(throw new RuntimeException("The given externalId doesn't exists")))
            .map(_.contactId)

        case None =>
          Future
            .successful(Contact.Id.from(request.contactId).toOption)
            .toFutureEither(
              new RuntimeException("The contactId is required, if it was provided, it's an invalid value")
            )
      }

      lazy val json =
        io.circe.parser.parse(request.credentialData).getOrElse(throw new RuntimeException("Invalid json"))

      val result = for {
        contactId <- subjectIdF
        model =
          request
            .into[CreateGenericCredential]
            .withFieldConst(_.issuedBy, Institution.Id(issuerId))
            .withFieldConst(_.subjectId, contactId)
            .withFieldConst(_.credentialData, json)
            .enableUnsafeOption
            .transform

        created <-
          credentialsRepository
            .create(model)
            .map(genericCredentialToProto)
      } yield console_api.CreateGenericCredentialResponse().withGenericCredential(created)

      result.failOnLeft(identity)
    }

  override def getGenericCredentials(
      request: GetGenericCredentialsRequest
  ): Future[GetGenericCredentialsResponse] =
    authenticatedHandler("getGenericCredentials", request) { issuerId =>
      val lastSeenCredential = GenericCredential.Id.from(request.lastSeenCredentialId).toOption
      credentialsRepository
        .getBy(Institution.Id(issuerId), request.limit, lastSeenCredential)
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

  private def authenticatedHandler[Request <: scalapb.GeneratedMessage, Response <: scalapb.GeneratedMessage](
      methodName: String,
      request: Request
  )(
      block: UUID => FutureEither[Nothing, Response]
  ): Future[Response] = {
    authenticator.authenticated(methodName, request) { participantId =>
      block(participantId.uuid).value
        .map {
          case Right(x) => x
          case Left(e) => throw new RuntimeException(s"FAILED: $e")
        }
    }
  }

  override def getContactCredentials(request: GetContactCredentialsRequest): Future[GetContactCredentialsResponse] = {
    def f(institutionId: Institution.Id): Future[GetContactCredentialsResponse] = {
      for {
        contactId <- Future.fromTry(Contact.Id.from(request.contactId))
        response <-
          credentialsRepository
            .getBy(institutionId, contactId)
            .map { list =>
              console_api.GetContactCredentialsResponse(list.map(genericCredentialToProto))
            }
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield response
    }

    authenticator.authenticated("getSubjectCredentials", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def shareCredential(request: ShareCredentialRequest): Future[ShareCredentialResponse] = {
    def f(institutionId: Institution.Id): Future[ShareCredentialResponse] = {
      for {
        credentialId <- Future.fromTry(GenericCredential.Id.from(request.cmanagerCredentialId))
        response <-
          credentialsRepository
            .markAsShared(institutionId, credentialId)
            .map { _ =>
              console_api.ShareCredentialResponse()
            }
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield response
    }

    authenticator.authenticated("shareCredential", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  /** Retrieves node information associated to a credential
    */
  override def getBlockchainData(request: GetBlockchainDataRequest): Future[GetBlockchainDataResponse] = {
    authenticator.authenticated("getBlockchainData", request) { _ =>
      // NOTE: Until we implement proper batching in the wallet, we will assume that the credential
      //       was published with a batch that contained the hash of the encodedSignedCredential
      //       as MerkleRoot in the IssueCredentialBatch operation. This allows us to compute the
      //       credential batch id without requesting the merkle root to the client.
      val batchIdF = Future.fromTry {
        val either = for {
          credential <- JsonBasedCredential.fromString(request.encodedSignedCredential)
          credentialHash = credential.hash
          issuerDID <- credential.content.issuerDid
        } yield CredentialBatchId.fromBatchData(issuerDID.suffix, MerkleRoot(credentialHash))
        either.toTry
      }

      for {
        batchId <- batchIdF
        response <-
          nodeService
            .getBatchState(
              node_api
                .GetBatchStateRequest()
                .withBatchId(batchId.id)
            )
      } yield response.publicationLedgerData.fold(GetBlockchainDataResponse())(ledgerData =>
        GetBlockchainDataResponse().withIssuanceProof(
          common_models
            .TransactionInfo()
            .withTransactionId(ledgerData.transactionId)
            .withLedger(ledgerData.ledger)
        )
      )
    }
  }

  /** Publishes a credential batch to the blockchain.
    * This method stores the published credentials on the database, and invokes the necessary
    * methods to get it published to the blockchain.
    */
  override def publishBatch(request: PublishBatchRequest): Future[PublishBatchResponse] = {
    def storeBatch(
        batchId: CredentialBatchId,
        signedIssueCredentialBatchOp: SignedAtalaOperation,
        transactionInfo: common_models.TransactionInfo
    ): Future[Unit] = {
      for {
        ledger <- CommonProtoCodecs.fromLedger(transactionInfo.ledger).tryF
        transactionId <-
          TransactionId
            .from(transactionInfo.transactionId)
            .getOrElse(throw new RuntimeException("Corrupted transaction ID"))
            .tryF
        (merkleRoot, did, operationHash) = extractValues(signedIssueCredentialBatchOp)
        computedBatchId = CredentialBatchId.fromBatchData(did.suffix, merkleRoot)
        // validation for sanity check
        // The `batchId` parameter is the id returned by the node.
        // We make this check to be sure that the node and the console are
        // using the same id (if this fails, they are using different versions
        // of the protocol)
        _ = assert(batchId == computedBatchId, "The batch id provided by the node does not match the one computed")
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
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield ()
    }

    authenticator.authenticated("publishBatch", request) { _ =>
      for {
        signedIssueCredentialBatchOp <-
          request.issueCredentialBatchOperation
            .getOrElse(throw new RuntimeException("Missing IssueCredentialBatch operation"))
            .tryF
        // Issue the batch in the Node
        credentialIssued <- nodeService.issueCredentialBatch {
          node_api
            .IssueCredentialBatchRequest()
            .withSignedOperation(signedIssueCredentialBatchOp)
        }
        returnedBatchId =
          CredentialBatchId
            .fromString(credentialIssued.batchId)
            .getOrElse(throw new RuntimeException("Node returned an invalid batch id"))
        transactionInfo =
          credentialIssued.transactionInfo
            .getOrElse(throw new RuntimeException("We could not generate a transaction"))
        // Update the database
        _ <- storeBatch(returnedBatchId, signedIssueCredentialBatchOp, transactionInfo)
      } yield console_api
        .PublishBatchResponse()
        .withTransactionInfo(transactionInfo)
        .withBatchId(credentialIssued.batchId)
    }
  }

  // This request stores in the console database the information associated to a credential. The endpoint assumes that
  // the credential has been publish in a batch though the PublishBatch endpoint
  override def storePublishedCredential(
      request: StorePublishedCredentialRequest
  ): Future[StorePublishedCredentialResponse] = {
    authenticator.authenticated("storePublishedCredential", request) { participantId =>
      for {
        issuerId <- Institution.Id(participantId.uuid).tryF
        encodedSignedCredential = request.encodedSignedCredential
        consoleCredentialId <- GenericCredential.Id(UUID.fromString(request.consoleCredentialId)).tryF
        _ = require(encodedSignedCredential.nonEmpty, "Empty encoded credential")
        maybeCredential <- credentialsRepository.getBy(consoleCredentialId).toFuture
        credential = maybeCredential.getOrElse(
          throw new RuntimeException(s"Credential with ID $consoleCredentialId does not exist")
        )
        // Verify issuer
        _ = require(credential.issuedBy == issuerId, "The credential was not issued by the specified issuer")
        batchId <- CredentialBatchId.fromString(request.batchId).get.tryF
        proof =
          MerkleInclusionProof
            .decode(request.encodedInclusionProof)
            .getOrElse(throw new RuntimeException("Empty inclusion proof"))
        _ <-
          credentialsRepository
            .storeCredentialPublicationData(
              issuerId,
              CredentialPublicationData(
                consoleCredentialId = consoleCredentialId,
                credentialBatchId = batchId,
                encodedSignedCredential = encodedSignedCredential,
                proofOfInclusion = proof
              )
            )
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield StorePublishedCredentialResponse()
    }
  }

  override def revokePublishedCredential(
      request: RevokePublishedCredentialRequest
  ): Future[RevokePublishedCredentialResponse] = {
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
  override def getLedgerData(request: GetLedgerDataRequest): Future[GetLedgerDataResponse] = ???
}
