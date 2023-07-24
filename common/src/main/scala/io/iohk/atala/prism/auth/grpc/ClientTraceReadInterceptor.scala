package io.iohk.atala.prism.auth.grpc

import io.grpc.ClientCall.Listener
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener
import io.grpc.{CallOptions, Channel, ClientCall, ClientInterceptor, Context, Metadata, MethodDescriptor}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationContext._

class ClientTraceReadInterceptor extends ClientInterceptor {
  override def interceptCall[ReqT, RespT](
      method: MethodDescriptor[ReqT, RespT],
      callOptions: CallOptions,
      next: Channel
  ): ClientCall[ReqT, RespT] = {

    new SimpleForwardingClientCall[ReqT, RespT](next.newCall(method, callOptions)) {
      override def start(responseListener: Listener[RespT], headers: Metadata): Unit = {
        val traceId = getTraceIdFromContext(Context.current())
        headers.put(TraceIdKeys.metadata, traceId.traceId)

        super.start(
          new SimpleForwardingClientCallListener[RespT](responseListener) {
            override def onHeaders(headers: Metadata): Unit = {
              super.onHeaders(headers)
            }
          },
          headers
        )
      }
    }
  }
}
