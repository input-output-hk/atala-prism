package io.iohk.atala.cvp.webextension.background.services.connector

import com.google.protobuf.ByteString
import io.grpc.stub.{ClientCallStreamObserver, StreamObserver}
import io.iohk.atala.cvp.webextension.background.models.console.ConsoleCredentialId
import io.iohk.atala.cvp.webextension.background.services._
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService.CredentialData
import io.iohk.atala.cvp.webextension.background.wallet.models.RoleHepler
import io.iohk.atala.cvp.webextension.common.ECKeyOperation._
import io.iohk.atala.cvp.webextension.common.models.Role
import io.iohk.atala.prism.protos.connector_api._
import io.iohk.atala.prism.protos.console_api.{
  PublishBatchRequest,
  PublishBatchResponse,
  StorePublishedCredentialRequest
}
import io.iohk.atala.prism.protos.{connector_api, console_api, node_models}
import scalapb.grpc.Channels
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.{
  MerkleInclusionProof,
  SHA256Digest,
  SHA256DigestCompanion
}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.identity.DID

import java.util.UUID
import io.iohk.atala.cvp.webextension.common.ECKeyOperation
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredentialCompanion
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.byteArray2Int8Array
import scala.util.Try

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
      .withCreateDidOperation(operation)
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
      issuingECKeyPair: ECKeyPair,
      masterECKeyPair: ECKeyPair,
      did: DID,
      signedCredentialStringRepresentation: String,
      batchId: CredentialBatchId,
      batchOperationHash: SHA256Digest,
      credentialId: UUID
  )(implicit ec: ExecutionContext): Future[ByteString] = {
    val credentialHashT = Try {
      JsonBasedCredentialCompanion
        .fromString(signedCredentialStringRepresentation)
        .hash()
    }

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
      signedOperation = signedAtalaOperation(ECKeyOperation.issuingKeyId, issuingECKeyPair, operation)
      request = {
        console_api
          .RevokePublishedCredentialRequest()
          .withRevokeCredentialsOperation(signedOperation)
          .withCredentialId(credentialId.toString)
      }
      requestMetadata = metadataForRequest(masterECKeyPair, did, request)
      response <- credentialsServiceApi.revokePublishedCredential(request, requestMetadata.toJSDictionary)
    } yield
      if (response.operationId.isEmpty) {
        throw new RuntimeException("The server didn't returned the expected operation identifier")
      } else {
        response.operationId
      }
  }

  def signAndPublishBatch(
      issuingECKeyPair: ECKeyPair,
      masterECKeyPair: ECKeyPair,
      did: DID,
      signingKeyId: String,
      credentialsData: List[CredentialData]
  )(implicit ec: ExecutionContext): Future[PublishBatchResponse] = {
    val (issuanceOperation, credentialsAndProofs) =
      issuerOperation(did, signingKeyId, issuingECKeyPair, credentialsData)
    val operation = signedAtalaOperation(signingKeyId, issuingECKeyPair, issuanceOperation)

    val publishBatchRequest = PublishBatchRequest()
      .withIssueCredentialBatchOperation(operation)

    val batchMetadata = metadataForRequest(masterECKeyPair, did, publishBatchRequest)

    for {
      // we first publish the batch
      batchResponse <- credentialsServiceApi.publishBatch(publishBatchRequest, batchMetadata.toJSDictionary)
      // now we store all the credentials data
      issuanceOperationHash = SHA256DigestCompanion.compute(byteArray2Int8Array(issuanceOperation.toByteArray))
      _ = println(s"issuanceOperationHash = '${issuanceOperationHash.hexValue()}'")
      _ = println(s"batchId = '${batchResponse.batchId}'")
      _ <- publishCredentials(
        masterECKeyPair,
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
          .withEncodedInclusionProof(proof.encode())

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
