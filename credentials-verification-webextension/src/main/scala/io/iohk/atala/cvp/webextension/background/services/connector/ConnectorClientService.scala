package io.iohk.atala.cvp.webextension.background.services.connector

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{EC, ECKeyPair, SHA256Digest}
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService._
import io.iohk.atala.cvp.webextension.common.ECKeyOperation._
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.cmanager_api.{PublishCredentialRequest, PublishCredentialResponse}
import io.iohk.atala.prism.protos.connector_api.{
  GetCurrentUserRequest,
  GetCurrentUserResponse,
  RegisterDIDRequest,
  RegisterDIDResponse
}
import io.iohk.atala.prism.protos.{cmanager_api, connector_api}
import scalapb.GeneratedMessage
import scalapb.grpc.Channels

import scala.concurrent.Future
import scala.scalajs.js.JSConverters._
import scala.util.{Failure, Success}

class ConnectorClientService(url: String) {
  private val connectorApi = connector_api.ConnectorServiceGrpcWeb.stub(Channels.grpcwebChannel(url))
  private val credentialsServiceApi = cmanager_api.CredentialsServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
    connectorApi.registerDID(request)
  }

  def getCurrentUser(ecKeyPair: ECKeyPair, did: DID): Future[GetCurrentUserResponse] = {
    val request = GetCurrentUserRequest()
    val metadata: Map[String, String] = metadataForRequest(ecKeyPair, did, request)
    connectorApi.getCurrentUser(request, metadata.toJSDictionary)
  }

  def signAndPublishCredential(
      ecKeyPair: ECKeyPair,
      did: DID,
      signingKeyId: String,
      credentialId: String, //Credential Manager Id
      credentialClaims: String
  ): Future[PublishCredentialResponse] = {
    val inputTry = for {
      (issuanceOperation, signedCredential) <- issuerOperation(did, signingKeyId, ecKeyPair, credentialClaims).toTry
      operation = signedAtalaOperation(ecKeyPair, issuanceOperation)
      sha256Hashed = SHA256Digest.compute(credentialClaims.getBytes)
      request = PublishCredentialRequest()
        .withCmanagerCredentialId(credentialId)
        .withEncodedSignedCredential(signedCredential)
        .withIssueCredentialOperation(operation)
        .withOperationHash(ByteString.copyFrom(sha256Hashed.value.toArray))
        .withNodeCredentialId(sha256Hashed.hexValue)
      metadata = metadataForRequest(ecKeyPair, did, request)
    } yield (request, metadata)

    inputTry match {
      case Success((request, metadata)) => credentialsServiceApi.publishCredential(request, metadata.toJSDictionary)
      case Failure(error) => Future.failed(error)
    }
  }
}

object ConnectorClientService {
  private val requestAuthenticator = new RequestAuthenticator(EC)

  def apply(url: String): ConnectorClientService = new ConnectorClientService(url)

  def metadataForRequest[Request <: GeneratedMessage](
      ecKeyPair: ECKeyPair,
      did: DID,
      request: Request
  ): Map[String, String] = {
    val signedConnectorRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, ecKeyPair.privateKey)
    Map(
      "did" -> did.value,
      "didKeyId" -> firstMasterKeyId,
      "didSignature" -> signedConnectorRequest.encodedSignature,
      "requestNonce" -> signedConnectorRequest.encodedRequestNonce
    )
  }
}
