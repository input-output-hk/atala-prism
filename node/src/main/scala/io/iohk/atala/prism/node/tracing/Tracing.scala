package io.iohk.atala.prism.node.tracing

import io.grpc.Context
import io.iohk.atala.prism.node.logging.TraceId

object Tracing {
  def trace[F[_], A](
      fa: TraceId => F[A]
  ): F[A] = {
    val ctx = Context.current()
    val traceId = Option(Context.key("trace-id").get(ctx)).map(TraceId(_)).getOrElse(TraceId.generateYOLO)
    fa(traceId)
  }
}
