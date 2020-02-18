package io.iohk.cvp.grpc

import java.util.{Base64, UUID}

import io.grpc.{Context, Metadata, Status, StatusRuntimeException}
import io.iohk.cvp.crypto.ECKeys.EncodedPublicKey
import io.iohk.cvp.models.ParticipantId

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

  def parseLegacyAuthenticationContext(): Option[GrpcAuthenticationHeader.Legacy] = {
    Option(UserIdKeys.context.get()).map(GrpcAuthenticationHeader.Legacy.apply)
  }

  def getPublicKeySignatureContext(headers: Metadata): Option[Context] = {
    (headers.getOpt(SignatureKeys), headers.getOpt(PublicKeyKeys)) match {
      case (Some(signatureStr), Some(publicKeyStr)) =>
        val signature = Base64.getUrlDecoder.decode(signatureStr)
        val publicKey = Base64.getUrlDecoder.decode(publicKeyStr)
        val ctx = Context
          .current()
          .withValue(SignatureKeys.context, signature)
          .withValue(PublicKeyKeys.context, publicKey)
        Some(ctx)

      case _ => None
    }
  }

  def parsePublicKeyAuthenticationHeader(): Option[GrpcAuthenticationHeader.PublicKeyBased] = {
    (Option(SignatureKeys.context.get()), Option(PublicKeyKeys.context.get())) match {
      case (Some(signatureStr), Some(publicKeyStr)) =>
        val encodedPublicKey = EncodedPublicKey(publicKeyStr.toVector)
        Some(GrpcAuthenticationHeader.PublicKeyBased(encodedPublicKey, signatureStr.toVector))

      case _ => None
    }
  }

  def getDIDSignatureContext(headers: Metadata): Option[Context] = {
    (headers.getOpt(DidKeys), headers.getOpt(DidKeyIdKeys), headers.getOpt(DidSignatureKeys)) match {
      case (Some(did), Some(keyId), Some(signatureStr)) =>
        val signature = Base64.getUrlDecoder.decode(signatureStr)
        val ctx = Context
          .current()
          .addValue(DidKeys, did)
          .addValue(DidKeyIdKeys, keyId)
          .addValue(DidSignatureKeys, signature)
        Some(ctx)

      case _ => None
    }
  }

  def parseDIDAuthenticationHeader(): Option[GrpcAuthenticationHeader.DIDBased] = {
    (Option(DidKeys.context.get()), Option(DidKeyIdKeys.context.get()), Option(DidSignatureKeys.context.get())) match {
      case (Some(did), Some(keyId), Some(signature)) =>
        val header = GrpcAuthenticationHeader.DIDBased(did = did, keyId = keyId, signature = signature.toVector)
        Some(header)

      case _ => None
    }
  }
}
