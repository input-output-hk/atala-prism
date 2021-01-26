package io.iohk.atala.cvp.webextension.background.services.connector

import com.google.protobuf.ByteString
import io.grpc.stub.{ClientCallStreamObserver, StreamObserver}
import io.iohk.atala.prism.crypto.{ECKeyPair, SHA256Digest}
import io.iohk.atala.cvp.webextension.background.services._
import io.iohk.atala.cvp.webextension.common.ECKeyOperation._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.console_api.{PublishCredentialRequest, PublishCredentialResponse}
import io.iohk.atala.prism.protos.connector_api.{
  GetCurrentUserRequest,
  GetCurrentUserResponse,
  GetMessageStreamRequest,
  GetMessageStreamResponse,
  RegisterDIDRequest,
  RegisterDIDResponse
}
import io.iohk.atala.prism.protos.{console_api, connector_api}
import scalapb.grpc.Channels

import scala.concurrent.Future
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

  def signAndPublishCredential(
      ecKeyPair: ECKeyPair,
      did: DID,
      signingKeyId: String,
      credentialId: String, //Credential Manager Id
      credentialClaims: String
  ): Future[PublishCredentialResponse] = {
    val (issuanceOperation, signedCredential, signedCredentialHash) =
      issuerOperation(did, signingKeyId, ecKeyPair, credentialClaims)
    val operation = signedAtalaOperation(ecKeyPair, issuanceOperation)
    val atalaOperationSHA256 = SHA256Digest.compute(issuanceOperation.toByteArray)
    // until we update the flow, we will post a credential batch where the merkle root
    // is the hash of the signed credential. The reason for this is that we need to be
    // able to query the credential transaction data associated to the issuance event
    // Eventually, we will update this to use real merkle trees and the system will share
    // merkle proofs as part of the information exchanged between parties.
    val merkleRoot = MerkleRoot(signedCredentialHash)
    val credentialBatchId = CredentialBatchId.fromBatchData(did.suffix, merkleRoot)
    val request = PublishCredentialRequest()
      .withCmanagerCredentialId(credentialId)
      .withEncodedSignedCredential(signedCredential)
      .withIssueCredentialOperation(operation)
      .withOperationHash(ByteString.copyFrom(atalaOperationSHA256.value.toArray))
      .withNodeCredentialId(credentialBatchId.id)
    val metadata = metadataForRequest(ecKeyPair, did, request)

    credentialsServiceApi.publishCredential(request, metadata.toJSDictionary)
  }

  def getMessageStream(
      ecKeyPair: ECKeyPair,
      did: DID,
      stramObserver: StreamObserver[GetMessageStreamResponse],
      lastSeenMessageId: String
  ): ClientCallStreamObserver = {
    val request = GetMessageStreamRequest(lastSeenMessageId = lastSeenMessageId)
    val metadata = metadataForRequest(ecKeyPair, did, request).toJSDictionary
    connectorApi.getMessageStream(
      request,
      metadata,
      stramObserver
    )
  }
}

object ConnectorClientService {
  def apply(url: String): ConnectorClientService = new ConnectorClientService(url)
}
