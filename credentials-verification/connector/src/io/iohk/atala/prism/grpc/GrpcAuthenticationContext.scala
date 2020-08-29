package io.iohk.atala.prism.grpc

import java.util.{Base64, UUID}

import io.grpc.{Context, Metadata, Status, StatusRuntimeException}
import io.iohk.atala.crypto.{EC, ECSignature}
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.connector.model.RequestNonce
import io.iohk.atala.prism.models.ParticipantId

import scala.util.Try

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
    def addValue[T](keys: GrpcMetadataContextKeys[T], value: T): Context = context.withValue(keys.context, value)

    /**
      * While the predefined way to get a value from the [[Context]] is to call the getters from
      * the [[Context.Key]], that's usually unsafe because the actual [[Context]] is not required and
      * it is retrieved from the [[java.lang.ThreadLocal]], which isn't safe to deal with on concurrent
      * environments (think about dealing with [[scala.concurrent.Future]]).
      *
      * This method allows avoiding the calls to the [[java.lang.ThreadLocal]], so, it's far safer than
      * the default way.
      *
      * @param keys the key used to retrieve the value from the context.
      * @tparam T the expected value type
      * @return the actual value
      */
    def getOpt[T](keys: GrpcMetadataContextKeys[T]): Option[T] = {
      Option(keys.context.get(context))
    }
  }

  // legacy authentication
  val UserIdKeys: GrpcMetadataContextKeys[ParticipantId] = GrpcMetadataContextKeys("userId")

  // public key authentication
  val SignatureKeys: GrpcMetadataContextKeys[Array[Byte]] = GrpcMetadataContextKeys("signature")
  val PublicKeyKeys: GrpcMetadataContextKeys[Array[Byte]] = GrpcMetadataContextKeys("publicKey")

  // DID authentication
  val DidKeys: GrpcMetadataContextKeys[String] = GrpcMetadataContextKeys("did")
  val DidKeyIdKeys: GrpcMetadataContextKeys[String] = GrpcMetadataContextKeys("didKeyId")
  val DidSignatureKeys: GrpcMetadataContextKeys[Array[Byte]] = GrpcMetadataContextKeys("didSignature")

  // used to prevent request replay attacks, this is a byte array to not worry about custom encoding
  // on different languages, like a string.
  val RequestNonceKeys: GrpcMetadataContextKeys[Array[Byte]] = GrpcMetadataContextKeys("requestNonce")

  private def unauthenticatedError(keys: GrpcMetadataContextKeys[_]): StatusRuntimeException = {
    Status.UNAUTHENTICATED
      .withDescription(s"${keys.metadata.name()} header missing or invalid")
      .asRuntimeException()
  }

  def participantId(): ParticipantId = {
    Option(UserIdKeys.context.get())
      .getOrElse(throw unauthenticatedError(UserIdKeys))
  }

  def getLegacyAuthenticationContext(headers: Metadata): Option[Context] = {
    headers
      .getOpt(UserIdKeys)
      .flatMap { userIdStr =>
        Try {
          new ParticipantId(UUID.fromString(userIdStr))
        }.toOption
      }
      .map { participantId =>
        Context.current().addValue(UserIdKeys, participantId)
      }
  }

  def parseLegacyAuthenticationContext(ctx: Context): Option[GrpcAuthenticationHeader.Legacy] = {
    ctx
      .getOpt(UserIdKeys)
      .map(GrpcAuthenticationHeader.Legacy.apply)
  }

  def getPublicKeySignatureContext(headers: Metadata): Option[Context] = {
    (headers.getOpt(RequestNonceKeys), headers.getOpt(SignatureKeys), headers.getOpt(PublicKeyKeys)) match {
      case (Some(requestNonceStr), Some(signatureStr), Some(publicKeyStr)) =>
        val signature = Base64.getUrlDecoder.decode(signatureStr)
        val publicKey = Base64.getUrlDecoder.decode(publicKeyStr)
        val requestNonce = Base64.getUrlDecoder.decode(requestNonceStr)
        val ctx = Context
          .current()
          .withValue(RequestNonceKeys.context, requestNonce)
          .withValue(SignatureKeys.context, signature)
          .withValue(PublicKeyKeys.context, publicKey)
        Some(ctx)

      case _ => None
    }
  }

  def parsePublicKeyAuthenticationHeader(ctx: Context): Option[GrpcAuthenticationHeader.PublicKeyBased] = {
    (ctx.getOpt(RequestNonceKeys), ctx.getOpt(SignatureKeys), ctx.getOpt(PublicKeyKeys)) match {
      case (Some(requestNonce), Some(signature), Some(encodedPublicKey)) =>
        val publicKey = EC.toPublicKey(encodedPublicKey)
        val header = GrpcAuthenticationHeader.PublicKeyBased(
          requestNonce = RequestNonce(requestNonce.toVector),
          publicKey = publicKey,
          signature = ECSignature(signature)
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
      case (Some(requestNonceStr), Some(did), Some(keyId), Some(signatureStr)) =>
        val signature = Base64.getUrlDecoder.decode(signatureStr)
        val requestNonce = Base64.getUrlDecoder.decode(requestNonceStr)
        val ctx = Context
          .current()
          .addValue(RequestNonceKeys, requestNonce)
          .addValue(DidKeys, did)
          .addValue(DidKeyIdKeys, keyId)
          .addValue(DidSignatureKeys, signature)
        Some(ctx)

      case _ => None
    }
  }

  def parseDIDAuthenticationHeader(ctx: Context): Option[GrpcAuthenticationHeader.DIDBased] = {
    (ctx.getOpt(RequestNonceKeys), ctx.getOpt(DidKeys), ctx.getOpt(DidKeyIdKeys), ctx.getOpt(DidSignatureKeys)) match {
      case (Some(requestNonce), Some(did), Some(keyId), Some(signature)) =>
        val header = GrpcAuthenticationHeader.DIDBased(
          requestNonce = RequestNonce(requestNonce.toVector),
          did = did,
          keyId = keyId,
          signature = ECSignature(signature)
        )
        Some(header)

      case _ => None
    }
  }
}
