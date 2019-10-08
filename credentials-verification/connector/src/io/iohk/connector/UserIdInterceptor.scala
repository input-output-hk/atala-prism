package io.iohk.connector

import java.util.UUID

import io.grpc.{Context, Contexts, Metadata, ServerCall, ServerCallHandler, ServerInterceptor}
import io.iohk.connector.model.ParticipantId

object UserIdInterceptor {
  val USER_ID_METADATA_KEY = Metadata.Key.of("userId", Metadata.ASCII_STRING_MARSHALLER)
  val USER_ID_CTX_KEY = Context.key[ParticipantId]("userId")
}

class UserIdInterceptor extends ServerInterceptor {
  import UserIdInterceptor._

  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    val userIdStr = headers.get(USER_ID_METADATA_KEY)
    val userId = new model.ParticipantId(UUID.fromString(userIdStr))
    val ctx = Context.current().withValue(USER_ID_CTX_KEY, userId)

    Contexts.interceptCall(ctx, call, headers, next)
  }
}
