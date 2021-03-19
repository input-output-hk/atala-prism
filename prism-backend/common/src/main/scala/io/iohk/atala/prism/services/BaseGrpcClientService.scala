package io.iohk.atala.prism.services

import java.util.Base64

import scala.concurrent.Future
import scala.util.Try

import io.grpc.Metadata
import io.grpc.stub.{AbstractStub, MetadataUtils}
import scalapb.GeneratedMessage
import monix.eval.Task
import org.slf4j.LoggerFactory
import cats.effect.Resource
import cats.data.OptionT
import com.google.protobuf.ByteString
import doobie.util.transactor.Transactor
import doobie.free.connection.ConnectionIO
import com.typesafe.config.Config
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECKeyPair}
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.services.BaseGrpcClientService.{
  AuthHeaders,
  BaseGrpcAuthConfig,
  DidBasedAuthConfig,
  PublicKeyBasedAuthConfig
}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.daos.DbConfigDao
import io.iohk.atala.prism.protos.{connector_api, node_models}

import doobie.implicits._

/**
  * Abstract service which provides support for DID based authentication for gRPC
  * and wraps response into [[monix.eval.Task]].
  */
abstract class BaseGrpcClientService[S <: AbstractStub[S]](
    stub: S,
    requestAuthenticator: RequestAuthenticator,
    authConfig: BaseGrpcAuthConfig
) {

  private val logger = LoggerFactory.getLogger(BaseGrpcClientService.getClass)

  /**
    * Perform gRPC call with DID based authentication.
    *
    * @param request gRPC request needed to create a signature
    * @param call a gRPC method that is performed on stub with proper authorization headers
    */
  def authenticatedCall[Response, Request <: GeneratedMessage](
      request: Request,
      call: S => Request => Future[Response]
  ): Task[Response] = {
    val newStub = MetadataUtils.attachHeaders(stub, signRequest(request))

    Task
      .deferFuture(call(newStub)(request))
      .onErrorHandleWith { error =>
        logger.error(s"Error occurred when calling grpc service: ${error.getMessage} cause: ${error.getCause}")
        Task.raiseError(error)
      }
  }

  private[services] def signRequest[Request <: GeneratedMessage](request: Request): Metadata = {
    val signature = requestAuthenticator.signConnectorRequest(
      request.toByteArray,
      authConfig.keys.privateKey
    )

    authConfig match {
      case DidBasedAuthConfig(did, didKeyId, _) =>
        createMetadataHeaders(
          AuthHeaders.DID -> did.value,
          AuthHeaders.DID_KEY_ID -> didKeyId,
          AuthHeaders.DID_SIGNATURE -> signature.encodedSignature,
          AuthHeaders.REQUEST_NONCE -> signature.encodedRequestNonce
        )
      case PublicKeyBasedAuthConfig(keyPair) =>
        createMetadataHeaders(
          AuthHeaders.PUBLIC_KEY -> Base64.getUrlEncoder.encodeToString(keyPair.publicKey.getEncoded),
          AuthHeaders.SIGNATURE -> signature.encodedSignature,
          AuthHeaders.REQUEST_NONCE -> signature.encodedRequestNonce
        )
    }
  }

  /**
    * Helper method to create authorization headers.
    *
    * @param headers pairs with header name -> value
    * @return [[Metadata]]
    */
  private[services] def createMetadataHeaders(headers: (Metadata.Key[String], String)*): Metadata = {
    val metadata = new Metadata

    headers.foreach {
      case (key, value) => metadata.put(key, value)
    }

    metadata
  }
}

object BaseGrpcClientService {

  abstract sealed class BaseGrpcAuthConfig(val keys: ECKeyPair)

  case class DidBasedAuthConfig(
      did: DID,
      didKeyId: String,
      didKeyPair: ECKeyPair
  ) extends BaseGrpcAuthConfig(didKeyPair)
  object DidBasedAuthConfig {

    /**
      * Names of the keys in the database.
      */
    object ConfigKeyNames {
      val DID = "did"
      val DID_KEY_ID = "did-key-id"
      val DID_PRIVATE_KEY = "did-private-key"
    }

    private val logger = LoggerFactory.getLogger(BaseGrpcClientService.getClass)

    def getOrCreate(
        applicationConfig: Config,
        tx: Transactor[Task],
        connector: connector_api.ConnectorServiceGrpc.ConnectorServiceStub
    ): Resource[Task, DidBasedAuthConfig] = {

      lazy val getFromApplicationConfig: Option[DidBasedAuthConfig] = {
        for {
          did <- Try(applicationConfig.getString(s"auth.${ConfigKeyNames.DID}")).toOption.map(DID.unsafeFromString)
          didKeyId <- Try(applicationConfig.getString(s"auth.${ConfigKeyNames.DID_KEY_ID}")).toOption
          didPrivateKey <- Try(applicationConfig.getString(s"auth.${ConfigKeyNames.DID_PRIVATE_KEY}")).toOption
          didKeyPair <- getECKeyPairFromString(didPrivateKey)
          _ = logger.info(s"DID for auth loaded from application.ini: ${did.toString}")
        } yield DidBasedAuthConfig(
          did = did,
          didKeyId = didKeyId,
          didKeyPair = didKeyPair
        )
      }

      lazy val getFromDb: Task[DidBasedAuthConfig] = {
        (for {
          did <- OptionT(DbConfigDao.get(ConfigKeyNames.DID)).map(DID.unsafeFromString)
          didKeyId <- OptionT(DbConfigDao.get(ConfigKeyNames.DID_KEY_ID))
          didPrivateKey <- OptionT(DbConfigDao.get(ConfigKeyNames.DID_PRIVATE_KEY))
          didKeyPair <- OptionT.fromOption[ConnectionIO](getECKeyPairFromString(didPrivateKey))
          _ = logger.info(s"DID for auth loaded from the DB config: ${did.toString}")
        } yield DidBasedAuthConfig(
          did = did,
          didKeyId = didKeyId,
          didKeyPair = didKeyPair
        )).transact(tx).getOrElseF(createNewDid.flatMap(saveAuthConfig))
      }

      def getECKeyPairFromString(encodedPrivateKey: String): Option[ECKeyPair] = {
        for {
          privateKey <- Try(EC.toPrivateKey(Base64.getUrlDecoder.decode(encodedPrivateKey))).toOption
          publicKey <- Try(EC.toPublicKeyFromPrivateKey(privateKey.getEncoded)).toOption
        } yield ECKeyPair(privateKey, publicKey)
      }

      def createNewDid: Task[DidBasedAuthConfig] = {
        val keyPair = EC.generateKeyPair()
        val publicKey = node_models.PublicKey(
          id = "master",
          usage = node_models.KeyUsage.MASTER_KEY,
          keyData = node_models.PublicKey.KeyData.EcKeyData(
            node_models.ECKeyData(
              curve = ECConfig.CURVE_NAME,
              x = ByteString.copyFrom(keyPair.publicKey.getCurvePoint.x.toByteArray),
              y = ByteString.copyFrom(keyPair.publicKey.getCurvePoint.y.toByteArray)
            )
          )
        )
        val createDidOp = node_models.CreateDIDOperation(
          didData = Some(
            node_models.DIDData(
              publicKeys = Seq(publicKey)
            )
          )
        )

        val atalaOp =
          node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))

        val signedAtalaOp = node_models.SignedAtalaOperation(
          signedWith = "master",
          operation = Some(atalaOp),
          signature = ByteString.copyFrom(EC.sign(atalaOp.toByteArray, keyPair.privateKey).data)
        )

        val request = connector_api
          .RegisterDIDRequest()
          .withCreateDIDOperation(signedAtalaOp)
          .withLogo(ByteString.EMPTY)
          .withName("auto-generated-did")
          .withRole(connector_api.RegisterDIDRequest.Role.issuer)

        for {
          authConfig <-
            Task
              .fromFuture(connector.registerDID(request))
              .map(response =>
                DidBasedAuthConfig(
                  did = DID.unsafeFromString(response.did),
                  didKeyId = "master",
                  didKeyPair = keyPair
                )
              )
          _ = logger.info(s"New DID for auth created: ${authConfig.did.toString}")
        } yield authConfig
      }

      def saveAuthConfig(authConfig: DidBasedAuthConfig): Task[DidBasedAuthConfig] = {
        (for {
          _ <- DbConfigDao.setIfNotExists(ConfigKeyNames.DID, authConfig.did.toString)
          _ <- DbConfigDao.setIfNotExists(ConfigKeyNames.DID_KEY_ID, authConfig.didKeyId)
          _ <- DbConfigDao.setIfNotExists(
            ConfigKeyNames.DID_PRIVATE_KEY,
            Base64.getUrlEncoder.encodeToString(authConfig.didKeyPair.privateKey.getEncoded)
          )
          // as we want to save DID once, even after save we have to get the current value again
        } yield ()).transact(tx).flatMap(_ => getFromDb)
      }

      Resource.liftF(
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
