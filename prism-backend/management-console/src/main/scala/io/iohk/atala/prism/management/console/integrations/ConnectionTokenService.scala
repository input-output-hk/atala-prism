package io.iohk.atala.prism.management.console.integrations

import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.iohk.atala.prism.management.console.models.GenerateConnectionTokenRequestMetadata
import io.iohk.atala.prism.protos.connector_api.{
  ConnectorServiceGrpc,
  GenerateConnectionTokenRequest,
  GenerateConnectionTokenResponse
}
import io.iohk.atala.prism.services.BaseGrpcClientService.AuthHeaders

import scala.concurrent.Future

trait ConnectionTokenService {
  def generateConnectionTokens(
      metadata: GenerateConnectionTokenRequestMetadata,
      count: Int
  ): Future[GenerateConnectionTokenResponse]
}

class ConnectionTokenServiceImpl(connectorService: ConnectorServiceGrpc.ConnectorServiceStub)
    extends ConnectionTokenService {
  def generateConnectionTokens(
      metadata: GenerateConnectionTokenRequestMetadata,
      count: Int
  ): Future[GenerateConnectionTokenResponse] = {
    val headers = createMetadataHeaders(
      AuthHeaders.DID -> metadata.did,
      AuthHeaders.DID_KEY_ID -> metadata.didKeyId,
      AuthHeaders.DID_SIGNATURE -> metadata.didSignature,
      AuthHeaders.REQUEST_NONCE -> metadata.requestNonce
    )

    val newStub = MetadataUtils.attachHeaders(connectorService, headers)

    newStub.generateConnectionToken(GenerateConnectionTokenRequest(count))
  }

  private def createMetadataHeaders(headers: (Metadata.Key[String], String)*): Metadata = {
    val metadata = new Metadata

    headers.foreach {
      case (key, value) => metadata.put(key, value)
    }

    metadata
  }
}
