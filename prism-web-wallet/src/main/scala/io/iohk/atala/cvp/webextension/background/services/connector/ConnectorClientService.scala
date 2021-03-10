package io.iohk.atala.cvp.webextension.background.services.connector

import com.google.protobuf.ByteString
import io.grpc.stub.{ClientCallStreamObserver, StreamObserver}
import io.iohk.atala.cvp.webextension.background.models.console.ConsoleCredentialId
import io.iohk.atala.cvp.webextension.background.services._
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService.CredentialData
import io.iohk.atala.cvp.webextension.background.wallet.models.RoleHepler
import io.iohk.atala.cvp.webextension.common.ECKeyOperation._
import io.iohk.atala.cvp.webextension.common.models.Role
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.crypto.{ECKeyPair, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.connector_api._
import io.iohk.atala.prism.protos.console_api.{
  PublishBatchRequest,
  PublishBatchResponse,
  StorePublishedCredentialRequest
}
import io.iohk.atala.prism.protos.{connector_api, console_api, node_models}
import scalapb.grpc.Channels

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.JSConverters._

class ConnectorClientService(url: String) {
  private val connectorApi = connector_api.ConnectorServiceGrpcWeb.stub(Channels.grpcwebChannel(url))
  private val credentialsServiceApi = console_api.CredentialsServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def registerDID(
      operation: node_models.SignedAtalaOperation,
      name: String,
      logo: Array[Byte],
      role: Role
  ): Future[RegisterDIDResponse] = {
    val request = connector_api
      .RegisterDIDRequest()
      .withCreateDIDOperation(operation)
      .withName(name)
      .withLogo(ByteString.copyFrom(logo))
      .withRole(RoleHepler.toConnectorApiRole(role))

    connectorApi.registerDID(request)
  }

  def getCurrentUser(ecKeyPair: ECKeyPair, did: DID): Future[GetCurrentUserResponse] = {
    val request = GetCurrentUserRequest()
    val metadata: Map[String, String] = metadataForRequest(ecKeyPair, did, request)
    connectorApi.getCurrentUser(request, metadata.toJSDictionary)
  }

  def revokeCredential(
      ecKeyPair: ECKeyPair,
      did: DID,
      signedCredentialStringRepresentation: String,
      batchId: CredentialBatchId,
      batchOperationHash: SHA256Digest,
      credentialId: UUID
  )(implicit ec: ExecutionContext): Future[io.iohk.atala.prism.protos.common_models.TransactionInfo] = {
    val credentialHashT = io.iohk.atala.prism.credentials.Credential
      .fromString(signedCredentialStringRepresentation)
      .map(_.hash)
      .toTry

    for {
      credentialHash <- Future.fromTry(credentialHashT)
      operation = {
        node_models
          .AtalaOperation(
            operation = node_models.AtalaOperation.Operation.RevokeCredentials(
              value = node_models
                .RevokeCredentialsOperation(
                  previousOperationHash = ByteString.copyFrom(batchOperationHash.value.toArray),
                  credentialBatchId = batchId.id,
                  credentialsToRevoke = List(ByteString.copyFrom(credentialHash.value.toArray))
                )
            )
          )
      }
      signedOperation = signedAtalaOperation(ecKeyPair, operation)
      request = {
        console_api
          .RevokePublishedCredentialRequest()
          .withRevokeCredentialsOperation(signedOperation)
          .withCredentialId(credentialId.toString)
      }
      requestMetadata = metadataForRequest(ecKeyPair, did, request)
      response <- credentialsServiceApi.revokePublishedCredential(request, requestMetadata.toJSDictionary)
    } yield response.transactionInfo.getOrElse(
      throw new RuntimeException("The server didn't returned the expected transaction info")
    )
  }

  def signAndPublishBatch(
      ecKeyPair: ECKeyPair,
      did: DID,
      signingKeyId: String,
      credentialsData: List[CredentialData]
  )(implicit ec: ExecutionContext): Future[PublishBatchResponse] = {
    val (issuanceOperation, credentialsAndProofs) =
      issuerOperation(did, signingKeyId, ecKeyPair, credentialsData)
    val operation = signedAtalaOperation(ecKeyPair, issuanceOperation)

    val publishBatchRequest = PublishBatchRequest()
      .withIssueCredentialBatchOperation(operation)

    val batchMetadata = metadataForRequest(ecKeyPair, did, publishBatchRequest)

    for {
      // we first publish the batch
      batchResponse <- credentialsServiceApi.publishBatch(publishBatchRequest, batchMetadata.toJSDictionary)
      // now we store all the credentials data
      issuanceOperationHash = SHA256Digest.compute(issuanceOperation.toByteArray)
      _ = println(s"issuanceOperationHash = '${issuanceOperationHash.hexValue}'")
      _ = println(s"batchId = '${batchResponse.batchId}'")
      _ <- publishCredentials(
        ecKeyPair,
        did,
        credentialsData.map(_.credentialId),
        credentialsAndProofs,
        batchResponse.batchId
      )
    } yield batchResponse
  }

  def getMessageStream(
      ecKeyPair: ECKeyPair,
      did: DID,
      streamObserver: StreamObserver[GetMessageStreamResponse],
      lastSeenMessageId: String
  ): ClientCallStreamObserver = {
    val request = GetMessageStreamRequest(lastSeenMessageId = lastSeenMessageId)
    val metadata = metadataForRequest(ecKeyPair, did, request).toJSDictionary
    connectorApi.getMessageStream(
      request,
      metadata,
      streamObserver
    )
  }

  private def publishCredentials(
      ecKeyPair: ECKeyPair,
      did: DID,
      credentialConsoleIds: List[ConsoleCredentialId],
      canonicalFormsAndProofs: List[(String, MerkleInclusionProof)],
      batchId: String
  )(implicit ex: ExecutionContext): Future[Unit] = {
    // we want to sequentially store the credentials information
    credentialConsoleIds.zip(canonicalFormsAndProofs).foldLeft(Future.unit) {
      case (acc, (consoleId, (encodedSignedCredential, proof))) =>
        val request = StorePublishedCredentialRequest()
          .withBatchId(batchId)
          .withEncodedSignedCredential(encodedSignedCredential)
          .withConsoleCredentialId(consoleId.id)
          .withEncodedInclusionProof(proof.encode)

        val metadata = metadataForRequest(ecKeyPair, did, request)

        for {
          _ <- credentialsServiceApi.storePublishedCredential(request, metadata.toJSDictionary)
          _ <- acc
        } yield ()
    }
  }
}

object ConnectorClientService {
  def apply(url: String): ConnectorClientService = new ConnectorClientService(url)

  case class CredentialData(
      credentialId: ConsoleCredentialId, // Credential Manager Id
      credentialClaims: String // JSON
  )
}
