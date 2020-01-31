package io.iohk.cvp.grpc

import java.util.{Base64, UUID}

import io.grpc._
import io.iohk.cvp.crypto.ECKeys._
import io.iohk.cvp.models.ParticipantId

object UserIdInterceptor {

  case class SignatureHeader(publicKey: EncodedPublicKey, signature: Vector[Byte])

  val USER_ID_METADATA_KEY = Metadata.Key.of("userId", Metadata.ASCII_STRING_MARSHALLER)
  val USER_ID_CTX_KEY = Context.key[Option[ParticipantId]]("userId")

  val SIGNATURE_METADATA_KEY = Metadata.Key.of("signature", Metadata.ASCII_STRING_MARSHALLER)
  val SIGNATURE_CTX_KEY = Context.key[Array[Byte]]("signature")

  val PUBLIC_METADATA_KEY = Metadata.Key.of("publicKey", Metadata.ASCII_STRING_MARSHALLER)
  val PUBLIC_CTX_KEY = Context.key[Array[Byte]]("publicKey")

  def participantId(): ParticipantId = {
    USER_ID_CTX_KEY
      .get()
      .getOrElse(throw Status.UNAUTHENTICATED.withDescription("userId header missing").asRuntimeException())
  }

  def getSignatureHeader(): Option[SignatureHeader] = {
    (Option(SIGNATURE_CTX_KEY.get()), Option(PUBLIC_CTX_KEY.get())) match {
      case (Some(signatureStr), Some(publicKeyStr)) => {
        val encodedPublicKey = EncodedPublicKey(publicKeyStr.toVector)
        Some(SignatureHeader(encodedPublicKey, signatureStr.toVector))
      }
      case _ => None
    }
  }

}

class UserIdInterceptor extends ServerInterceptor {
  import UserIdInterceptor._

  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {

    val context = getSignatureContext(headers) orElse getUserIdContext(headers) orElse getDefaultContext(headers) get

    Contexts.interceptCall(context, call, headers, next)

  }

  // Unauthenticated request default context
  private def getDefaultContext(headers: Metadata): Option[Context] = Some(Context.current())

  private def getUserIdContext(headers: Metadata): Option[Context] =
    Option(headers.get(USER_ID_METADATA_KEY)).map { userIdStr =>
      val participantId = new ParticipantId(UUID.fromString(userIdStr))
      Context.current().withValue(USER_ID_CTX_KEY, Some(participantId))
    }

  private def getSignatureContext(headers: Metadata): Option[Context] = {
    (Option(headers.get(SIGNATURE_METADATA_KEY)), Option(headers.get(PUBLIC_METADATA_KEY))) match {
      case (Some(signatureStr), Some(publicKeyStr)) => {
        val signature = Base64.getUrlDecoder.decode(signatureStr)
        val publicKey = Base64.getUrlDecoder.decode(publicKeyStr)
        val ctx = Context.current().withValues(SIGNATURE_CTX_KEY, signature, PUBLIC_CTX_KEY, publicKey)
        Some(ctx)
      }
      case (Some(signatureStr), None) =>
        throw Status.UNAUTHENTICATED.withDescription("publicKey header missing").asRuntimeException()
      case (None, Some(publicKeyStr)) =>
        throw Status.UNAUTHENTICATED.withDescription("signature header missing").asRuntimeException()
      case _ => None
    }
  }

}
