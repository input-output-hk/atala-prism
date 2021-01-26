package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.console.grpc.ProtoCodecs._
import io.iohk.atala.prism.console.models.{
  Contact,
  CreateGenericCredential,
  GenericCredential,
  Institution,
  PublishCredential
}
import io.iohk.atala.prism.console.repositories.{ContactsRepository, CredentialsRepository}
import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ProtoCodecs
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{console_api, node_api, common_models}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureOptionOps
import io.iohk.atala.prism.utils.syntax._
import io.scalaland.chimney.dsl._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CredentialsServiceImpl(
    credentialsRepository: CredentialsRepository,
    contactsRepository: ContactsRepository,
    authenticator: ConnectorAuthenticator,
    nodeService: NodeServiceGrpc.NodeService
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialsServiceGrpc.CredentialsService {

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

  /** Publish an encoded signed credential into the blockchain
    *
    * NOTE: We will update this method when we migrate to credential batching.
    *       Until then, this RPC will expect a signed IssueCredentialBatch instead of
    *       in the `issueCredentialOperation` field. This operation will contain the hash
    *       of the encodedSignedCredential instead of an actual merkle root.
    *       This hack will be changed when we implement actual batching, but we need it for
    *       now to allow retrieving credential timestamp data (to keep backward compatibility).
    */
  override def publishCredential(request: PublishCredentialRequest): Future[PublishCredentialResponse] = {
    authenticator.authenticated("publishCredential", request) { participantId =>
      for {
        issuerId <- Institution.Id(participantId.uuid).tryF
        credentialId = GenericCredential.Id.unsafeFrom(request.cmanagerCredentialId)
        batchProtocolId = request.nodeCredentialId
        issuanceOperationHash = SHA256Digest(request.operationHash.toByteArray.toVector)
        encodedSignedCredential = request.encodedSignedCredential
        signedIssueCredentialBatchOp =
          request.issueCredentialOperation
            .getOrElse(throw new RuntimeException("Missing IssueCredential operation"))
        // validation for sanity check
        (merkleRoot, did, operationHash) = extractValues(signedIssueCredentialBatchOp)
        batchId = CredentialBatchId.fromBatchData(did.suffix, merkleRoot)
        _ = require(
          batchProtocolId == batchId.id,
          "Invalid batch protocol id"
        )
        _ = require(
          issuanceOperationHash == operationHash,
          "Operation hash does not match the provided operation"
        )
        _ = require(encodedSignedCredential.nonEmpty, "Empty encoded credential")
        // Verify issuer
        maybeCredential <- credentialsRepository.getBy(credentialId).toFuture
        credential =
          maybeCredential.getOrElse(throw new RuntimeException(s"Credential with ID $credentialId does not exist"))
        _ = require(credential.issuedBy == issuerId, "The credential was not issued by the specified issuer")
        // Issue the credential in the Node
        credentialIssued <- nodeService.issueCredentialBatch {
          node_api
            .IssueCredentialBatchRequest()
            .withSignedOperation(signedIssueCredentialBatchOp)
        }
        transactionInfo =
          credentialIssued.transactionInfo.getOrElse(throw new RuntimeException("Credential issues has no transaction"))
        // Update the database
        _ <-
          credentialsRepository
            .storePublicationData(
              issuerId,
              PublishCredential(
                credentialId,
                issuanceOperationHash,
                batchProtocolId,
                encodedSignedCredential,
                ProtoCodecs.fromTransactionInfo(transactionInfo)
              )
            )
            .value
            .map {
              case Right(x) => x
              case Left(e) => throw new RuntimeException(s"FAILED: $e")
            }
      } yield console_api.PublishCredentialResponse().withTransactionInfo(transactionInfo)
    }
  }

  private def extractValues(signedAtalaOperation: SignedAtalaOperation): (MerkleRoot, DID, SHA256Digest) = {
    val maybePair = for {
      atalaOperation <- signedAtalaOperation.operation
      opHash = SHA256Digest.compute(atalaOperation.toByteArray)
      issueCredentialBatch <- atalaOperation.operation.issueCredentialBatch
      credentialBatchData <- issueCredentialBatch.credentialBatchData
      did = DID.buildPrismDID(credentialBatchData.issuerDID)
      merkleRoot = MerkleRoot(SHA256Digest(credentialBatchData.merkleRoot.toByteArray.toVector))
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
}
