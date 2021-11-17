package io.iohk.atala.prism.tracing

import io.grpc.Context
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.logging.TraceId

import scala.concurrent.Future

object Tracing {
  def trace[A](fa: TraceId => Future[A]): Future[A] = {
    val traceId = GrpcAuthenticationHeaderParser.getTraceId(Context.current())
    fa(traceId)
  }
}
