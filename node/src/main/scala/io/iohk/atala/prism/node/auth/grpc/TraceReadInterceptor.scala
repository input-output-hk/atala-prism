package io.iohk.atala.prism.node.auth.grpc

import io.grpc.{Contexts, Metadata, ServerCall, ServerCallHandler, ServerInterceptor}

class TraceReadInterceptor extends ServerInterceptor {
  import GrpcAuthenticationContext._

  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {

    val context = getTraceIdContext(headers)

    Contexts.interceptCall(context, call, headers, next)
  }

}
