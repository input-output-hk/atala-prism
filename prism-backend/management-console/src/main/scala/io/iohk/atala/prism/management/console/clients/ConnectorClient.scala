package io.iohk.atala.prism.management.console.clients

import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader.PublishedDIDBased
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.{EC, ECPrivateKey, ECSignature}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.identity.DID.masterKeyId
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_api._
import io.iohk.atala.prism.protos.connector_models.ContactConnection
import io.iohk.atala.prism.services.BaseGrpcClientService.AuthHeaders
import io.iohk.atala.prism.util
import io.iohk.atala.prism.util.BytesOps
import io.iohk.atala.prism.utils.{Base64Utils, GrpcUtils}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait ConnectorClient {
  // the whole request metadata is required because we expect the client invoking the RPC to sign
  // the connector request beforehand, which allows our service to invoke the connector on behalf
  // of such client.
  def generateConnectionTokens(
      metadata: GrpcAuthenticationHeader.DIDBased,
      count: Int
  ): Future[Seq[ConnectionToken]]

  def sendMessages(
      request: SendMessagesRequest,
      metadata: GrpcAuthenticationHeader.DIDBased
  ): Future[SendMessagesResponse]

  def getConnectionStatus(tokens: Seq[ConnectionToken]): Future[Seq[ContactConnection]]
}

object ConnectorClient {

  case class Config(host: String, port: Int, whitelistedDID: DID, didPrivateKey: ECPrivateKey) {
    override def toString: String = {
      s"""ConnectorClient.Config:
         |host = $host
         |port = $port
         |whitelistedDID = $whitelistedDID
         |didPrivateKey = ${util.StringUtils.masked(didPrivateKey.getHexEncoded)}""".stripMargin
    }
  }
  object Config {
    def apply(typesafe: com.typesafe.config.Config): Config = {
      def unsafe = {
        val host = typesafe.getString("host")
        val port = typesafe.getInt("port")
        val whitelistedDID = DID
          .fromString(typesafe.getString("did"))
          .getOrElse {
            throw new RuntimeException("Failed to load the connector's whitelisted DID, which is required to invoke it")
          }

        val didPrivateKey = EC.toPrivateKey(
          BytesOps.hexToBytes(typesafe.getString("didPrivateKeyHex"))
        )

        Config(host = host, port = port, whitelistedDID = whitelistedDID, didPrivateKey = didPrivateKey)
      }

      Try(unsafe) match {
        case Failure(exception) =>
          throw new RuntimeException(s"Failed to load connector config: ${exception.getMessage}", exception)
        case Success(value) => value
      }
    }
  }

  def apply(config: Config)(implicit ec: ExecutionContext): ConnectorClient = {
    val connectorContactsService = GrpcUtils.createPlaintextStub(
      host = config.host,
      port = config.port,
      stub = ContactConnectionServiceGrpc.stub
    )

    val connectorService = GrpcUtils
      .createPlaintextStub(host = config.host, port = config.port, stub = ConnectorServiceGrpc.stub)

    val requestAuthenticator = new RequestAuthenticator(EC)

    def requestSigner(request: scalapb.GeneratedMessage): GrpcAuthenticationHeader.DIDBased = {

      val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, config.didPrivateKey)
      PublishedDIDBased(
        did = DID.unsafeFromString(config.whitelistedDID.value),
        keyId = masterKeyId,
        requestNonce = RequestNonce(signedRequest.requestNonce.toVector),
        signature = ECSignature(signedRequest.signature)
      )
    }
    new ConnectorClient.GrpcImpl(connectorService, connectorContactsService)(requestSigner)
  }

  class GrpcImpl(
      connectorService: ConnectorServiceGrpc.ConnectorServiceStub,
      contactConnectionService: ContactConnectionServiceGrpc.ContactConnectionServiceStub
  )(requestSigner: scalapb.GeneratedMessage => GrpcAuthenticationHeader.DIDBased)(implicit ec: ExecutionContext)
      extends ConnectorClient {

    override def generateConnectionTokens(
        metadata: GrpcAuthenticationHeader.DIDBased,
        count: Int
    ): Future[Seq[ConnectionToken]] = {
      val headers = createMetadataHeaders(metadata)
      val newStub = MetadataUtils.attachHeaders(connectorService, headers)

      newStub
        .generateConnectionToken(GenerateConnectionTokenRequest(count))
        .map(_.tokens.map(ConnectionToken.apply))
    }

    override def sendMessages(
        request: SendMessagesRequest,
        metadata: GrpcAuthenticationHeader.DIDBased
    ): Future[SendMessagesResponse] = {
      val headers = createMetadataHeaders(metadata)
      val newStub = MetadataUtils.attachHeaders(connectorService, headers)

      newStub.sendMessages(request)
    }

    override def getConnectionStatus(tokens: Seq[ConnectionToken]): Future[Seq[ContactConnection]] = {
      val request = ConnectionsStatusRequest()
        .withConnectionTokens(tokens.map(_.token))
      val signedMetadata = requestSigner(request)
      val headers = createMetadataHeaders(signedMetadata)
      val newStub = MetadataUtils.attachHeaders(contactConnectionService, headers)

      newStub
        .getConnectionStatus(request)
        .map(_.connections)
    }

    private def createMetadataHeaders(headers: GrpcAuthenticationHeader.DIDBased): Metadata = {
      val metadata = new Metadata

      List(
        AuthHeaders.DID -> headers.did.toString,
        AuthHeaders.DID_KEY_ID -> headers.keyId,
        AuthHeaders.DID_SIGNATURE -> Base64Utils.encodeURL(headers.signature.data),
        AuthHeaders.REQUEST_NONCE -> Base64Utils.encodeURL(headers.requestNonce.bytes.toArray)
      ).foreach {
        case (key, value) => metadata.put(key, value)
      }

      metadata
    }
  }
}
