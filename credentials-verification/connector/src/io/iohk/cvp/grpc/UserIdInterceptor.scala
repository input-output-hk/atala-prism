package io.iohk.cvp.grpc

import java.util.UUID

import io.grpc._
import io.iohk.cvp.models.ParticipantId

object UserIdInterceptor {
  val USER_ID_METADATA_KEY = Metadata.Key.of("userId", Metadata.ASCII_STRING_MARSHALLER)
  val USER_ID_CTX_KEY = Context.key[Option[ParticipantId]]("userId")
  def participantId(): ParticipantId = {
    USER_ID_CTX_KEY
      .get()
      .getOrElse(throw Status.UNAUTHENTICATED.withDescription("userId header missing").asRuntimeException())
  }
}

class UserIdInterceptor extends ServerInterceptor {
  import UserIdInterceptor._

  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {

    Option(headers.get(USER_ID_METADATA_KEY)) match {
      case Some(userIdStr) => {
        val participantId = new ParticipantId(UUID.fromString(userIdStr))
        val ctx = Context.current().withValue(USER_ID_CTX_KEY, Some(participantId))
        Contexts.interceptCall(ctx, call, headers, next)
      }
      case _ => {
        val ctx = Context.current().withValue(USER_ID_CTX_KEY, None)
        Contexts.interceptCall(ctx, call, headers, next)
      }
    }
  }
}
