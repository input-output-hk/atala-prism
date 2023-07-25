package io.iohk.atala.prism.auth.grpc

import java.util.Base64
import io.grpc.{Context, Metadata}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.auth.model.{AuthToken, RequestNonce}
import io.iohk.atala.prism.identity.{CanonicalPrismDid, LongFormPrismDid, PrismDid}

import scala.util.{Failure, Success, Try}
import io.iohk.atala.prism.logging.TraceId

private[grpc] object GrpcAuthenticationContext {
  // Extension methods to deal with gRPC Metadata in the Scala way
  implicit class RichMetadata(val value: Metadata) extends AnyVal {
    def getOpt(keys: GrpcMetadataContextKeys[_]): Option[String] = {
      Option(value.get(keys.metadata))
    }
  }

  // Extension methods to deal with gRPC Context in the Scala way
  implicit class RichContext(val context: Context) extends AnyVal {
    // For some reason the compiler complains if the method is named withValue
    def addValue[T](keys: GrpcMetadataContextKeys[T], value: T): Context =
      context.withValue(keys.context, value)

    /** While the predefined way to get a value from the [[Context]] is to call the getters from the [[Context.Key]],
      * that's usually unsafe because the actual [[Context]] is not required and it is retrieved from the
      * [[java.lang.ThreadLocal]], which isn't safe to deal with on concurrent environments (think about dealing with
      * [[scala.concurrent.Future]]).
      *
      * This method allows avoiding the calls to the [[java.lang.ThreadLocal]], so, it's far safer than the default way.
      *
      * @param keys
      *   the key used to retrieve the value from the context.
      * @tparam T
      *   the expected value type
      * @return
      *   the actual value
      */
    def getOpt[T](keys: GrpcMetadataContextKeys[T]): Option[T] = {
      Option(keys.context.get(context))
    }
  }

  // public key authentication
  val SignatureKeys: GrpcMetadataContextKeys[Array[Byte]] =
    GrpcMetadataContextKeys("signature")
  val PublicKeyKeys: GrpcMetadataContextKeys[Array[Byte]] =
    GrpcMetadataContextKeys("public-key")

  // DID authentication
  val DidKeys: GrpcMetadataContextKeys[String] = GrpcMetadataContextKeys("did")
  val DidKeyIdKeys: GrpcMetadataContextKeys[String] = GrpcMetadataContextKeys(
    "did-key-id"
  )
  val DidSignatureKeys: GrpcMetadataContextKeys[Array[Byte]] =
    GrpcMetadataContextKeys("did-signature")

  // used to prevent request replay attacks, this is a byte array to not worry about custom encoding
  // on different languages, like a string.
  val RequestNonceKeys: GrpcMetadataContextKeys[Array[Byte]] =
    GrpcMetadataContextKeys("request-nonce")

  // tracing
  val TraceIdKeys: GrpcMetadataContextKeys[String] = GrpcMetadataContextKeys(
    "trace-id"
  )

  // Prism auth Token
  val AuthTokenKeys: GrpcMetadataContextKeys[String] = GrpcMetadataContextKeys(
    "prism-auth-token"
  )

  def getAuthTokenFromContext(ctx: Context): Option[AuthToken] =
    ctx.getOpt(AuthTokenKeys).map(AuthToken)

  def getAuthTokenFromMetadata(headers: Metadata): Option[AuthToken] =
    headers.getOpt(AuthTokenKeys).map(AuthToken)

  def getAuthTokenContext(headers: Metadata): Option[Context] = {
    val authToken = headers.getOpt(AuthTokenKeys)
    authToken.map {
      Context
        .current()
        .withValue(AuthTokenKeys.context, _)
    }
  }

  def getTraceIdFromContext(ctx: Context): TraceId =
    ctx.getOpt(TraceIdKeys).map(TraceId(_)).getOrElse(TraceId.generateYOLO)

  def getTraceIdFromMetadata(headers: Metadata): TraceId =
    headers.getOpt(TraceIdKeys).map(TraceId(_)).getOrElse(TraceId.generateYOLO)

  def getTraceIdContext(headers: Metadata): Context = {
    val traceId = headers.getOpt(TraceIdKeys).map(TraceId(_)).getOrElse(TraceId.generateYOLO)

    Context
      .current()
      .withValue(TraceIdKeys.context, traceId.traceId)
  }

  def getPublicKeySignatureContext(headers: Metadata): Option[Context] = {
    (
      headers.getOpt(RequestNonceKeys),
      headers.getOpt(SignatureKeys),
      headers.getOpt(PublicKeyKeys)
    ) match {
      case (Some(requestNonceStr), Some(signatureStr), Some(publicKeyStr)) =>
        val signature = Base64.getUrlDecoder.decode(signatureStr)
        val publicKey = Base64.getUrlDecoder.decode(publicKeyStr)
        val requestNonce = Base64.getUrlDecoder.decode(requestNonceStr)
        val traceId = getTraceIdFromMetadata(headers)
        val ctx = Context
          .current()
          .withValue(RequestNonceKeys.context, requestNonce)
          .withValue(SignatureKeys.context, signature)
          .withValue(PublicKeyKeys.context, publicKey)
          .withValue(TraceIdKeys.context, traceId.traceId)
        Some(ctx)

      case _ => None
    }
  }

  def parsePublicKeyAuthenticationHeader(
      ctx: Context
  ): Option[GrpcAuthenticationHeader.PublicKeyBased] = {
    (
      ctx.getOpt(RequestNonceKeys),
      ctx.getOpt(SignatureKeys),
      ctx.getOpt(PublicKeyKeys)
    ) match {
      case (Some(requestNonce), Some(signature), Some(encodedPublicKey)) =>
        val publicKey = EC.toPublicKeyFromBytes(encodedPublicKey)
        val header = GrpcAuthenticationHeader.PublicKeyBased(
          requestNonce = RequestNonce(requestNonce.toVector),
          publicKey = publicKey,
          signature = new ECSignature(signature)
        )
        Some(header)

      case _ => None
    }
  }

  def getDIDSignatureContext(headers: Metadata): Option[Context] = {
    (
      headers.getOpt(RequestNonceKeys),
      headers.getOpt(DidKeys),
      headers.getOpt(DidKeyIdKeys),
      headers.getOpt(DidSignatureKeys)
    ) match {
      case (
            Some(requestNonceStr),
            Some(did),
            Some(keyId),
            Some(signatureStr)
          ) =>
        val signature = Base64.getUrlDecoder.decode(signatureStr)
        val requestNonce = Base64.getUrlDecoder.decode(requestNonceStr)
        val traceId = getTraceIdFromMetadata(headers)
        val ctx = Context
          .current()
          .addValue(RequestNonceKeys, requestNonce)
          .addValue(DidKeys, did)
          .addValue(DidKeyIdKeys, keyId)
          .addValue(DidSignatureKeys, signature)
          .addValue(TraceIdKeys, traceId.traceId)
        Some(ctx)

      case _ => None
    }
  }

  def parseDIDAuthenticationHeader(
      ctx: Context
  ): Option[GrpcAuthenticationHeader.DIDBased] = {
    (
      ctx.getOpt(RequestNonceKeys),
      ctx.getOpt(DidKeys),
      ctx.getOpt(DidKeyIdKeys),
      ctx.getOpt(DidSignatureKeys)
    ) match {
      case (Some(requestNonce), Some(didRaw), Some(keyId), Some(signature)) =>
        val didOpt = Try(PrismDid.fromString(didRaw))
        didOpt match {
          case Success(did) =>
            did match {
              case _: CanonicalPrismDid =>
                Some(
                  GrpcAuthenticationHeader.PublishedDIDBased(
                    requestNonce = RequestNonce(requestNonce.toVector),
                    did = did,
                    keyId = keyId,
                    signature = new ECSignature(signature)
                  )
                )
              case _: LongFormPrismDid =>
                Some(
                  GrpcAuthenticationHeader.UnpublishedDIDBased(
                    requestNonce = RequestNonce(requestNonce.toVector),
                    did = did,
                    keyId = keyId,
                    signature = new ECSignature(signature)
                  )
                )
              case _ =>
                throw new IllegalStateException("Unknown type of DID")
            }
          case Failure(_) =>
            None
        }
      case _ => None
    }
  }
}
