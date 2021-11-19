package io.iohk.atala.prism.auth.grpc

import io.grpc.Context
import io.iohk.atala.prism.logging.TraceId

trait GrpcAuthenticationHeaderParser {

  /** Get the authentication header from the current context.
    */
  def parse(ctx: Context): Option[GrpcAuthenticationHeader] = {
    GrpcAuthenticationContext
      .parseDIDAuthenticationHeader(ctx)
      .orElse(GrpcAuthenticationContext.parsePublicKeyAuthenticationHeader(ctx))
  }

  def getTraceId(ctx: Context): TraceId =
    GrpcAuthenticationContext.getTraceIdFromContext(ctx)

}

object GrpcAuthenticationHeaderParser extends GrpcAuthenticationHeaderParser {
  def grpcHeader[F[_], A](fa: Option[GrpcAuthenticationHeader] => F[A]): F[A] = {
    val grpcHeaderOp = GrpcAuthenticationHeaderParser.parse(Context.current())
    fa(grpcHeaderOp)
  }
}
