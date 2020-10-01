package io.iohk.atala.mirror.services

import scala.concurrent.Future

import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import scalapb.GeneratedMessage
import monix.eval.Task

import io.iohk.prism.protos.connector_api._
import io.iohk.atala.requests.RequestAuthenticator
import io.iohk.atala.mirror.config.ConnectorConfig

class ConnectorClientService(
    connector: ConnectorServiceGrpc.ConnectorServiceStub,
    requestAuthenticator: RequestAuthenticator,
    connectorConfig: ConnectorConfig
) {

  def generateConnectionToken: Task[GenerateConnectionTokenResponse] = {
    val request = GenerateConnectionTokenRequest()

    authenticatedCall(request, _.generateConnectionToken)
  }

  /**
    * Perform gRPC call to Connector with DID based authentication.
    *
    * @param request gRPC request needed to create a signature
    * @param call a gRPC method that is performed on stub with proper authorization headers
    */
  def authenticatedCall[Response, Request <: GeneratedMessage](
      request: Request,
      call: ConnectorServiceGrpc.ConnectorServiceStub => Request => Future[Response]
  ): Task[Response] = {
    val signature = requestAuthenticator.signConnectorRequest(
      request.toByteArray,
      connectorConfig.didKeyPair.privateKey
    )

    val headers = ConnectorClientService.createMetadataHeaders(
      "didSignature" -> signature.encodedSignature,
      "requestNonce" -> signature.encodedRequestNonce
    )

    val stub = MetadataUtils.attachHeaders(connector, headers)

    Task.fromFuture(call(stub)(request))
  }

}

object ConnectorClientService {

  /**
    * Helper method to create authorization headers.
    *
    * @param headers pairs with header name -> value
    * @return [[Metadata]]
    */
  def createMetadataHeaders(headers: (String, String)*): Metadata = {
    val metadata = new Metadata

    headers.foreach {
      case (key, value) =>
        metadata.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value)
    }

    metadata
  }
}
