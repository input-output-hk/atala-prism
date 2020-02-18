package io.iohk.cvp.grpc

import io.grpc._

class GrpcAuthenticatorInterceptor extends ServerInterceptor {

  import GrpcAuthenticationContext._

  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {

    val context = getDIDSignatureContext(headers)
      .orElse(getPublicKeySignatureContext(headers))
      .orElse(getLegacyAuthenticationContext(headers))
      .getOrElse(getDefaultContext(headers))

    Contexts.interceptCall(context, call, headers, next)
  }

  // Unauthenticated request default context
  private def getDefaultContext(headers: Metadata): Context = Context.current()

}
