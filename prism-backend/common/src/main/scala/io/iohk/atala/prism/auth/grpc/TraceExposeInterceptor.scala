package io.iohk.atala.prism.auth.grpc

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall
import io.grpc.{Metadata, ServerCall, ServerCallHandler, ServerInterceptor}

class TraceExposeInterceptor extends ServerInterceptor {
  override def interceptCall[ReqT, RespT](
      call: ServerCall[ReqT, RespT],
      headers: Metadata,
      next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    import GrpcAuthenticationContext._

    next.startCall(
      new SimpleForwardingServerCall[ReqT, RespT](call) {
        override def sendHeaders(responseHeaders: Metadata): Unit = {
          val traceId = getTraceIdFromMetadata(headers)

          responseHeaders.put(TraceIdKeys.metadata, traceId.traceId)
          super.sendHeaders(responseHeaders)
        }
      },
      headers
    )
  }
}
