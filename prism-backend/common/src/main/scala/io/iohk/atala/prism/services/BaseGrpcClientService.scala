package io.iohk.atala.prism.services

import java.util.Base64
import scala.concurrent.Future
import scala.util.Try
import io.grpc.Metadata
import io.grpc.stub.{AbstractStub, MetadataUtils, StreamObserver}
import scalapb.GeneratedMessage
import monix.eval.Task
import org.slf4j.LoggerFactory
import cats.effect.Resource
import cats.data.OptionT
import com.google.protobuf.ByteString
import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO
import com.typesafe.config.Config
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECKeyPair
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.services.BaseGrpcClientService.{
  AuthHeaders,
  BaseGrpcAuthConfig,
  DidBasedAuthConfig,
  PublicKeyBasedAuthConfig
}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.daos.DbConfigDao
import io.iohk.atala.prism.protos.{connector_api, node_models}
import doobie.implicits._

/** Abstract service which provides support for DID based authentication for gRPC and wraps response into
  * [[monix.eval.Task]].
  */
abstract class BaseGrpcClientService[S <: AbstractStub[S]](
    stub: S,
    requestAuthenticator: RequestAuthenticator,
    authConfig: BaseGrpcAuthConfig
) {

  private val logger = LoggerFactory.getLogger(BaseGrpcClientService.getClass)

  /** Perform gRPC call with DID based authentication.
    *
    * @param request
    *   gRPC request needed to create a signature
    * @param call
    *   a gRPC method that is performed on stub with proper authorization headers
    */
  def authenticatedCall[Response, Request <: GeneratedMessage](
      request: Request,
      call: S => Request => Future[Response]
  ): Task[Response] = {
    val newStub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(signRequest(request)))

    Task
      .deferFuture(call(newStub)(request))
      .onErrorHandleWith { error =>
        logger.error(s"Error occurred when calling grpc service: ${error.getMessage} cause: ${error.getCause}")
        Task.raiseError(error)
      }
  }

  /** Perform gRPC call with DID based authentication for stream processing.
    *
    * @param request
    *   gRPC request needed to create a signature
    * @param call
    *   a gRPC method that is performed on stub with proper authorization headers
    */
  def authenticatedCallStream[Request <: GeneratedMessage, StreamElement](
      request: Request,
      streamObserver: StreamObserver[StreamElement],
      call: S => (Request, StreamObserver[StreamElement]) => Unit
  ): Unit = {
    val newStub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(signRequest(request)))
    call(newStub)(request, streamObserver)
  }

  private[services] def signRequest[Request <: GeneratedMessage](request: Request): Metadata = {
    val signature = requestAuthenticator.signConnectorRequest(
      request.toByteArray,
      authConfig.keys.getPrivateKey
    )

    authConfig match {
      case DidBasedAuthConfig(did, didKeyId, _, _, _) =>
        createMetadataHeaders(
          AuthHeaders.DID -> did.getValue,
          AuthHeaders.DID_KEY_ID -> didKeyId,
          AuthHeaders.DID_SIGNATURE -> signature.encodedSignature,
          AuthHeaders.REQUEST_NONCE -> signature.encodedRequestNonce
        )
      case PublicKeyBasedAuthConfig(keyPair) =>
        createMetadataHeaders(
          AuthHeaders.PUBLIC_KEY -> Base64.getUrlEncoder.encodeToString(keyPair.getPublicKey.getEncoded),
          AuthHeaders.SIGNATURE -> signature.encodedSignature,
          AuthHeaders.REQUEST_NONCE -> signature.encodedRequestNonce
        )
    }
  }

  /** Helper method to create authorization headers.
    *
    * @param headers
    *   pairs with header name -> value
    * @return
    *   [[Metadata]]
    */
  private[services] def createMetadataHeaders(headers: (Metadata.Key[String], String)*): Metadata = {
    val metadata = new Metadata

    headers.foreach { case (key, value) =>
      metadata.put(key, value)
    }

    metadata
  }
}

object BaseGrpcClientService {

  abstract sealed class BaseGrpcAuthConfig(val keys: ECKeyPair)

  case class DidBasedAuthConfig(
      did: DID,
      didMasterKeyId: String,
      didMasterKeyPair: ECKeyPair,
      didIssuingKeyId: String,
      didIssuingKeyPair: ECKeyPair
  ) extends BaseGrpcAuthConfig(didMasterKeyPair)
  object DidBasedAuthConfig {

    /** Names of the keys in the database.
      */
    object ConfigKeyNames {
      val DID = "did"
      val DID_MASTER_KEY_ID = "did-master-key-id"
      val DID_MASTER_PRIVATE_KEY = "did-master-private-key"
      val DID_ISSUING_KEY_ID = "did-issuing-key-id"
      val DID_ISSUING_PRIVATE_KEY = "did-issuing-private-key"
    }

    private val logger = LoggerFactory.getLogger(BaseGrpcClientService.getClass)

    def getOrCreate(
        applicationConfig: Config,
        tx: Transactor[Task],
        connector: connector_api.ConnectorServiceGrpc.ConnectorServiceStub
    ): Resource[Task, DidBasedAuthConfig] = {

      lazy val getFromApplicationConfig: Option[DidBasedAuthConfig] = {
        for {
          did <- Try(applicationConfig.getString(s"auth.${ConfigKeyNames.DID}")).toOption.map(DID.fromString)

          didMasterKeyId <- Try(applicationConfig.getString(s"auth.${ConfigKeyNames.DID_MASTER_KEY_ID}")).toOption
          didMasterPrivateKey <- Try(
            applicationConfig.getString(s"auth.${ConfigKeyNames.DID_MASTER_PRIVATE_KEY}")
          ).toOption
          didMasterKeyPair <- getECKeyPairFromString(didMasterPrivateKey)

          didIssuingKeyId <- Try(applicationConfig.getString(s"auth.${ConfigKeyNames.DID_ISSUING_KEY_ID}")).toOption
          didIssuingPrivateKey <- Try(
            applicationConfig.getString(s"auth.${ConfigKeyNames.DID_ISSUING_PRIVATE_KEY}")
          ).toOption
          didIssuingKeyPair <- getECKeyPairFromString(didIssuingPrivateKey)

          _ = logger.info(s"DID for auth loaded from application.ini: ${did.toString}")
        } yield DidBasedAuthConfig(
          did = did,
          didMasterKeyId = didMasterKeyId,
          didMasterKeyPair = didMasterKeyPair,
          didIssuingKeyId = didIssuingKeyId,
          didIssuingKeyPair = didIssuingKeyPair
        )
      }

      lazy val getFromDb: Task[DidBasedAuthConfig] = {
        (for {
          did <- OptionT(DbConfigDao.get(ConfigKeyNames.DID)).map(DID.fromString)

          didMasterKeyId <- OptionT(DbConfigDao.get(ConfigKeyNames.DID_MASTER_KEY_ID))
          didMasterPrivateKey <- OptionT(DbConfigDao.get(ConfigKeyNames.DID_MASTER_PRIVATE_KEY))
          didMasterKeyPair <- OptionT.fromOption[ConnectionIO](getECKeyPairFromString(didMasterPrivateKey))

          didIssuingKeyId <- OptionT(DbConfigDao.get(ConfigKeyNames.DID_ISSUING_KEY_ID))
          didIssuingPrivateKey <- OptionT(DbConfigDao.get(ConfigKeyNames.DID_ISSUING_PRIVATE_KEY))
          didIssuingKeyPair <- OptionT.fromOption[ConnectionIO](getECKeyPairFromString(didIssuingPrivateKey))

          _ = logger.info(s"DID for auth loaded from the DB config: ${did.toString}")
        } yield DidBasedAuthConfig(
          did = did,
          didMasterKeyId = didMasterKeyId,
          didMasterKeyPair = didMasterKeyPair,
          didIssuingKeyId = didIssuingKeyId,
          didIssuingKeyPair = didIssuingKeyPair
        )).transact(tx).getOrElseF(createNewDid.flatMap(saveAuthConfig))
      }

      def getECKeyPairFromString(encodedPrivateKey: String): Option[ECKeyPair] = {
        for {
          privateKey <- Try(EC.toPrivateKeyFromBytes(Base64.getUrlDecoder.decode(encodedPrivateKey))).toOption
          publicKey <- Try(EC.toPublicKeyFromPrivateKey(privateKey)).toOption
        } yield new ECKeyPair(publicKey, privateKey)
      }

      def createNewDid: Task[DidBasedAuthConfig] = {
        val masterKeyId = "master"
        val masterKeyPair = EC.generateKeyPair()
        val publicMasterKey = node_models.PublicKey(
          id = masterKeyId,
          usage = node_models.KeyUsage.MASTER_KEY,
          keyData = node_models.PublicKey.KeyData.EcKeyData(
            node_models.ECKeyData(
              curve = ECConfig.getCURVE_NAME,
              x = ByteString.copyFrom(masterKeyPair.getPublicKey.getCurvePoint.getX.bytes()),
              y = ByteString.copyFrom(masterKeyPair.getPublicKey.getCurvePoint.getY.bytes())
            )
          )
        )
        val issuingKeyId = "issuance"
        val issuingKeyPair = EC.generateKeyPair()
        val publicIssuingKey = node_models.PublicKey(
          id = issuingKeyId,
          usage = node_models.KeyUsage.ISSUING_KEY,
          keyData = node_models.PublicKey.KeyData.EcKeyData(
            node_models.ECKeyData(
              curve = ECConfig.getCURVE_NAME,
              x = ByteString.copyFrom(issuingKeyPair.getPublicKey.getCurvePoint.getX.bytes()),
              y = ByteString.copyFrom(issuingKeyPair.getPublicKey.getCurvePoint.getY.bytes())
            )
          )
        )
        val createDidOp = node_models.CreateDIDOperation(
          didData = Some(
            node_models.CreateDIDOperation.DIDCreationData(
              publicKeys = Seq(publicMasterKey, publicIssuingKey)
            )
          )
        )

        val atalaOp =
          node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))

        val signedAtalaOp = node_models.SignedAtalaOperation(
          signedWith = masterKeyId,
          operation = Some(atalaOp),
          signature = ByteString.copyFrom(EC.signBytes(atalaOp.toByteArray, masterKeyPair.getPrivateKey).getData)
        )

        val request = connector_api
          .RegisterDIDRequest()
          .withCreateDidOperation(signedAtalaOp)
          .withLogo(ByteString.EMPTY)
          .withName("auto-generated-did")
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)

        for {
          authConfig <-
            Task
              .fromFuture(connector.registerDID(request))
              .map(response =>
                DidBasedAuthConfig(
                  did = DID.fromString(response.did),
                  didMasterKeyId = masterKeyId,
                  didMasterKeyPair = masterKeyPair,
                  didIssuingKeyId = issuingKeyId,
                  didIssuingKeyPair = issuingKeyPair
                )
              )
          _ = logger.info(s"New DID for auth created: ${authConfig.did.toString}")
        } yield authConfig
      }

      def saveAuthConfig(authConfig: DidBasedAuthConfig): Task[DidBasedAuthConfig] = {
        (for {
          _ <- DbConfigDao.setIfNotExists(ConfigKeyNames.DID, authConfig.did.toString)
          _ <- DbConfigDao.setIfNotExists(ConfigKeyNames.DID_MASTER_KEY_ID, authConfig.didMasterKeyId)
          _ <- DbConfigDao.setIfNotExists(
            ConfigKeyNames.DID_MASTER_PRIVATE_KEY,
            Base64.getUrlEncoder.encodeToString(authConfig.didMasterKeyPair.getPrivateKey.getEncoded)
          )
          _ <- DbConfigDao.setIfNotExists(ConfigKeyNames.DID_ISSUING_KEY_ID, authConfig.didIssuingKeyId)
          _ <- DbConfigDao.setIfNotExists(
            ConfigKeyNames.DID_ISSUING_PRIVATE_KEY,
            Base64.getUrlEncoder.encodeToString(authConfig.didIssuingKeyPair.getPrivateKey.getEncoded)
          )
          // as we want to save DID once, even after save we have to get the current value again
        } yield ()).transact(tx).flatMap(_ => getFromDb)
      }

      Resource.eval(
        Task(getFromApplicationConfig).flatMap {
          case None => getFromDb
          case Some(authConfig) => Task.pure(authConfig)
        }
      )
    }
  }

  case class PublicKeyBasedAuthConfig(keyPair: ECKeyPair) extends BaseGrpcAuthConfig(keyPair)

  object AuthHeaders {
    // DID based
    val DID = Metadata.Key.of("did", Metadata.ASCII_STRING_MARSHALLER)
    val DID_KEY_ID = Metadata.Key.of("didKeyId", Metadata.ASCII_STRING_MARSHALLER)
    val DID_SIGNATURE = Metadata.Key.of("didSignature", Metadata.ASCII_STRING_MARSHALLER)
    // PublicKey based
    val PUBLIC_KEY = Metadata.Key.of("publicKey", Metadata.ASCII_STRING_MARSHALLER)
    val SIGNATURE = Metadata.Key.of("signature", Metadata.ASCII_STRING_MARSHALLER)
    // Common
    val REQUEST_NONCE = Metadata.Key.of("requestNonce", Metadata.ASCII_STRING_MARSHALLER)
  }
}
