package io.iohk.atala.cvp.webextension.background.services.connector

import java.util.Base64

import io.iohk.atala.crypto.{EC, ECKeyPair, ECPrivateKey, SHA256Digest}
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService._
import io.iohk.atala.cvp.webextension.common.ECKeyOperation._
import io.iohk.atala.cvp.webextension.common.models.RequestNonce
import io.iohk.prism.protos.cmanager_api.{PublishCredentialRequest, PublishCredentialResponse}
import io.iohk.prism.protos.connector_api.{
  GetCurrentUserRequest,
  GetCurrentUserResponse,
  RegisterDIDRequest,
  RegisterDIDResponse
}
import io.iohk.prism.protos.{cmanager_api, connector_api}
import scalapb.GeneratedMessage
import scalapb.grpc.Channels

import scala.concurrent.Future
import scala.scalajs.js.JSConverters._
import scala.util.{Success, Failure}

class ConnectorClientService(url: String) {
  val connectorApi = connector_api.ConnectorServiceGrpcWeb.stub(Channels.grpcwebChannel(url))
  val credentialsServiceApi = cmanager_api.CredentialsServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def registerDID(request: RegisterDIDRequest): Future[RegisterDIDResponse] = {
    connectorApi.registerDID(request)
  }

  def getCurrentUser(ecKeyPair: ECKeyPair, did: String): Future[GetCurrentUserResponse] = {
    val request = GetCurrentUserRequest()
    val metadata: Map[String, String] = metadataForRequest(ecKeyPair, did, request)
    connectorApi.getCurrentUser(request, metadata.toJSDictionary)
  }

  def signAndPublishCredential(
      ecKeyPair: ECKeyPair,
      did: String,
      signingKeyId: String,
      credentialId: String, //Credential Manager Id
      credentialClaims: String
  ): Future[PublishCredentialResponse] = {
    val inputTry = for {
      issuanceOperation <- issuerOperation(did, signingKeyId, ecKeyPair, credentialClaims).toTry
      operation = signedAtalaOperation(ecKeyPair, issuanceOperation)
      sha256Hashed = SHA256Digest.compute(credentialClaims.getBytes).hexValue
      request =
        PublishCredentialRequest()
          .withCmanagerCredentialId(credentialId)
          .withEncodedSignedCredential(generateUrlEncodedSignature(credentialClaims.getBytes, ecKeyPair.privateKey))
          .withIssueCredentialOperation(operation)
          .withOperationHash(sha256Hashed) //TODO Fix this as discussed when Protobuf is updated
          .withNodeCredentialId(sha256Hashed)
      metadata = metadataForRequest(ecKeyPair, did, request)
    } yield (request, metadata)

    inputTry match {
      case Success((request, metadata)) => credentialsServiceApi.publishCredential(request, metadata.toJSDictionary)
      case Failure(error) => Future.failed(error)
    }
  }
}

object ConnectorClientService {

  def apply(url: String): ConnectorClientService = new ConnectorClientService(url)

  private def generateUrlEncodedSignature(data: Array[Byte], privateKey: ECPrivateKey): String = {
    val signature = EC.sign(data, privateKey)
    Base64.getUrlEncoder.encodeToString(signature.data)
  }

  def getUrlEncodedRequestNonce(requestNonce: RequestNonce): String = {
    Base64.getUrlEncoder.encodeToString(requestNonce.bytes)
  }

  def metadataForRequest[Request <: GeneratedMessage](
      ecKeyPair: ECKeyPair,
      did: String,
      request: Request
  ): Map[String, String] = {
    val requestNonce = RequestNonce()
    val didKeyValue = "did" -> did
    val didKeyId = "didKeyId" -> firstMasterKeyId
    val didSignature =
      "didSignature" -> generateUrlEncodedSignature(requestNonce + request.toByteArray, ecKeyPair.privateKey)
    val requestNoncePair = "requestNonce" -> getUrlEncodedRequestNonce(requestNonce)
    val metadata: Map[String, String] = Map(didKeyValue, didKeyId, didSignature, requestNoncePair)
    metadata
  }
}
