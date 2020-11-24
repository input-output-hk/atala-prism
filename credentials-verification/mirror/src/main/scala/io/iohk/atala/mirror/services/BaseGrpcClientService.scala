package io.iohk.atala.mirror.services

import java.util.Base64
import scala.concurrent.Future
import io.grpc.Metadata
import io.grpc.stub.{AbstractStub, MetadataUtils}
import scalapb.GeneratedMessage
import monix.eval.Task
import io.iohk.atala.prism.crypto.ECKeyPair
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.mirror.services.BaseGrpcClientService.{
  AuthHeaders,
  BaseGrpcAuthConfig,
  DidBasedAuthConfig,
  PublicKeyBasedAuthConfig
}
import io.iohk.atala.prism.identity.DID

/**
  * Abstract service which provides support for DID based authentication for gRPC
  * and wraps response into [[monix.eval.Task]].
  */
abstract class BaseGrpcClientService[S <: AbstractStub[S]](
    stub: S,
    requestAuthenticator: RequestAuthenticator,
    authConfig: BaseGrpcAuthConfig
) {

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

    Task.fromFuture(call(newStub)(request))
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
