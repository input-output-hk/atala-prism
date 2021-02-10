package io.iohk.atala.cvp.webextension.background.services.connector

import io.grpc.stub.{ClientCallStreamObserver, StreamObserver}
import io.iohk.atala.cvp.webextension.background.models.console.ConsoleCredentialId
import io.iohk.atala.prism.crypto.ECKeyPair
import io.iohk.atala.cvp.webextension.background.services._
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService.CredentialData
import io.iohk.atala.cvp.webextension.common.ECKeyOperation._
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.console_api.{
  PublishBatchRequest,
  PublishBatchResponse,
  StorePublishedCredentialRequest
}
import io.iohk.atala.prism.protos.connector_api.{
  GetCurrentUserRequest,
  GetCurrentUserResponse,
  GetMessageStreamRequest,
  GetMessageStreamResponse,
  RegisterDIDRequest,
  RegisterDIDResponse
}
import io.iohk.atala.prism.protos.{connector_api, console_api}
import scalapb.grpc.Channels

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.JSConverters._

class ConnectorClientService(url: String) {
  private val connectorApi = connector_api.ConnectorServiceGrpcWeb.stub(Channels.grpcwebChannel(url))
  private val credentialsServiceApi = console_api.CredentialsServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
    connectorApi.registerDID(request)
  }

  def getCurrentUser(ecKeyPair: ECKeyPair, did: DID): Future[GetCurrentUserResponse] = {
    val request = GetCurrentUserRequest()
    val metadata: Map[String, String] = metadataForRequest(ecKeyPair, did, request)
    connectorApi.getCurrentUser(request, metadata.toJSDictionary)
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
      credentialId: ConsoleCredentialId, //Credential Manager Id
      credentialClaims: String // JSON
  )
}
