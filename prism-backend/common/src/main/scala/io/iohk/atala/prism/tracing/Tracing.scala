package io.iohk.atala.prism.tracing

import io.grpc.Context
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.logging.TraceId

object Tracing {
  def trace[F[_], A](fa: TraceId => F[A]): F[A] = {
    val traceId = GrpcAuthenticationHeaderParser.getTraceId(Context.current())
    fa(traceId)
  }
}
