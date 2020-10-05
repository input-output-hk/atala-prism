package io.iohk.atala.mirror.services

import monix.eval.Task

import io.iohk.prism.protos.connector_api._
import io.iohk.prism.protos.credential_models.{AtalaMessage, ProofRequest}
import io.iohk.atala.requests.RequestAuthenticator
import io.iohk.atala.mirror.config.ConnectorConfig
import io.iohk.atala.mirror.models.Connection.{ConnectionId, ConnectionToken}
import io.iohk.atala.mirror.models.CredentialProofRequestType

class ConnectorClientService(
    connector: ConnectorServiceGrpc.ConnectorServiceStub,
    requestAuthenticator: RequestAuthenticator,
    connectorConfig: ConnectorConfig
) extends BaseGrpcClientService(connector, requestAuthenticator, connectorConfig.authConfig) {

  def generateConnectionToken: Task[GenerateConnectionTokenResponse] = {
    val request = GenerateConnectionTokenRequest()

    authenticatedCall(request, _.generateConnectionToken)
  }

  def requestCredential(
      connectionId: ConnectionId,
      connectionToken: ConnectionToken,
      credentialProofRequestTypes: Seq[CredentialProofRequestType]
  ): Task[SendMessageResponse] = {
    val proofRequest =
      AtalaMessage().withProofRequest(ProofRequest(credentialProofRequestTypes.map(_.typeId), connectionToken.token))

    val request = SendMessageRequest(connectionId.uuid.toString, proofRequest.toByteString)

    authenticatedCall(request, _.sendMessage)
  }

}
