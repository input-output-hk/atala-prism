package io.iohk.atala.prism.auth.grpc

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
      .getOrElse(getDefaultContext())

    Contexts.interceptCall(context, call, headers, next)
  }

  // Unauthenticated request default context
  private def getDefaultContext(): Context = Context.current()

}
