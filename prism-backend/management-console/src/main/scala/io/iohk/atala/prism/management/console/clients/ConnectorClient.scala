package io.iohk.atala.prism.management.console.clients

import cats.{Applicative, Functor}
import cats.effect.{MonadThrow, Resource}
import cats.syntax.apply._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import cats.syntax.flatMap._
import derevo.tagless.applyK
import derevo.derive
import io.grpc.stub.MetadataUtils
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader.PublishedDIDBased
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.crypto.{EC => ECScalaSDK}
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.identity.DID.masterKeyId
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_api._
import io.iohk.atala.prism.protos.connector_models.ContactConnection
import io.iohk.atala.prism.util
import io.iohk.atala.prism.util.BytesOps
import io.iohk.atala.prism.utils.GrpcUtils
import io.iohk.atala.prism.logging.GeneralLoggableInstances._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import io.iohk.atala.prism.interop.toScalaSDK._
import tofu.Execute
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.logging._

@derive(applyK)
trait ConnectorClient[F[_]] {
  // the whole request metadata is required because we expect the client invoking the RPC to sign
  // the connector request beforehand, which allows our service to invoke the connector on behalf
  // of such client.
  def generateConnectionTokens(
      header: GrpcAuthenticationHeader.DIDBased,
      count: Int
  ): F[Seq[ConnectionToken]]

  def sendMessages(
      request: SendMessagesRequest,
      header: GrpcAuthenticationHeader.DIDBased
  ): F[SendMessagesResponse]

  def getConnectionStatus(tokens: Seq[ConnectionToken]): F[Seq[ContactConnection]]
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

  def apply[F[_]: Execute: MonadThrow, R[_]: Functor](
      config: Config,
      logs: Logs[R, F]
  )(implicit ec: ExecutionContext): R[ConnectorClient[F]] =
    for {
      serviceLogs <- logs.service[ConnectorClient[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, ConnectorClient[F]] = serviceLogs
      val connectorContactsService = GrpcUtils.createPlaintextStub(
        host = config.host,
        port = config.port,
        stub = ContactConnectionServiceGrpc.stub
      )

      val connectorService = GrpcUtils
        .createPlaintextStub(host = config.host, port = config.port, stub = ConnectorServiceGrpc.stub)

      val requestAuthenticator = new RequestAuthenticator(ECScalaSDK)

      def requestSigner(request: scalapb.GeneratedMessage): GrpcAuthenticationHeader.DIDBased = {

        val signedRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, config.didPrivateKey.asScala)
        PublishedDIDBased(
          did = DID.unsafeFromString(config.whitelistedDID.value),
          keyId = masterKeyId,
          requestNonce = RequestNonce(signedRequest.encodedRequestNonce.getBytes.toVector),
          signature = new ECSignature(signedRequest.encodedSignature.getBytes)
        )
      }

      val mid: ConnectorClient[Mid[F, *]] = new ConnectorClientLogs[F]
      mid attach new ConnectorClient.GrpcImpl[F](connectorService, connectorContactsService)(requestSigner)
    }

  def makeResource[F[_]: Execute: MonadThrow, R[_]: Applicative: Functor](
      config: Config,
      logs: Logs[R, F]
  )(implicit ec: ExecutionContext): Resource[R, ConnectorClient[F]] =
    Resource.eval(ConnectorClient[F, R](config, logs))

  private final class GrpcImpl[F[_]: Execute](
      connectorService: ConnectorServiceGrpc.ConnectorServiceStub,
      contactConnectionService: ContactConnectionServiceGrpc.ContactConnectionServiceStub
  )(requestSigner: scalapb.GeneratedMessage => GrpcAuthenticationHeader.DIDBased)(implicit
      ec: ExecutionContext,
      ex: Execute[F]
  ) extends ConnectorClient[F] {

    override def generateConnectionTokens(
        header: GrpcAuthenticationHeader.DIDBased,
        count: Int
    ): F[Seq[ConnectionToken]] = {
      val metadata = header.toMetadata
      val newStub = MetadataUtils.attachHeaders(connectorService, metadata)

      ex.deferFuture(
        newStub
          .generateConnectionToken(GenerateConnectionTokenRequest(count))
          .map(_.tokens.map(ConnectionToken.apply))
      )
    }

    override def sendMessages(
        request: SendMessagesRequest,
        header: GrpcAuthenticationHeader.DIDBased
    ): F[SendMessagesResponse] = {
      val metadata = header.toMetadata
      val newStub = MetadataUtils.attachHeaders(connectorService, metadata)

      ex.deferFuture(newStub.sendMessages(request))
    }

    override def getConnectionStatus(tokens: Seq[ConnectionToken]): F[Seq[ContactConnection]] = {
      val request = ConnectionsStatusRequest()
        .withConnectionTokens(tokens.map(_.token))
      val header = requestSigner(request)
      val metadata = header.toMetadata
      val newStub = MetadataUtils.attachHeaders(contactConnectionService, metadata)

      ex.deferFuture(
        newStub
          .getConnectionStatus(request)
          .map(_.connections)
      )
    }
  }
}

private[clients] final class ConnectorClientLogs[F[_]: ServiceLogging[*[_], ConnectorClient[F]]: MonadThrow]
    extends ConnectorClient[Mid[F, *]] {
  override def generateConnectionTokens(
      header: GrpcAuthenticationHeader.DIDBased,
      count: Int
  ): Mid[F, Seq[ConnectionToken]] =
    in =>
      info"generating connection tokens for ${header.did.getCanonicalSuffix}" *> in
        .flatTap(list => info"generating connection tokens - successfully done got ${list.size} entities")
        .onError(errorCause"encountered an error while generating connection tokens" (_))

  override def sendMessages(
      request: SendMessagesRequest,
      header: GrpcAuthenticationHeader.DIDBased
  ): Mid[F, SendMessagesResponse] =
    in =>
      info"sending messages ${header.did.getCanonicalSuffix}" *> in
        .flatTap(response => info"sending messages - successfully done got ${response.ids.size} ids")
        .onError(errorCause"encountered an error while sending messages" (_))

  override def getConnectionStatus(tokens: Seq[ConnectionToken]): Mid[F, Seq[ContactConnection]] =
    in =>
      info"getting connection status for ${tokens.size} token(s)" *> in
        .flatTap(response =>
          info"getting connection status - successfully done got ${response.size} contact connection(s)"
        )
        .onError(errorCause"encountered an error while getting connection status" (_))
}
