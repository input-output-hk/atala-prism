package io.iohk.atala.prism.node.tracing

import io.grpc.Context
import io.iohk.atala.prism.node.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.node.logging.TraceId

object Tracing {
  def trace[F[_], A](
      fa: TraceId => F[A],
      parser: GrpcAuthenticationHeaderParser = GrpcAuthenticationHeaderParser
  ): F[A] = {
    val traceId = parser.getTraceId(Context.current())
    fa(traceId)
  }
}
