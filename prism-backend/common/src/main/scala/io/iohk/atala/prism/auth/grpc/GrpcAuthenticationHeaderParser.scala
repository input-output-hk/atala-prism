package io.iohk.atala.prism.auth.grpc

import io.grpc.Context
import io.iohk.atala.prism.logging.TraceId

import scala.concurrent.Future

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
  def grpcHeader[A](fa: Option[GrpcAuthenticationHeader] => Future[A]): Future[A] = {
    val grpcHeaderOp = GrpcAuthenticationHeaderParser.parse(Context.current())
    fa(grpcHeaderOp)
  }
}
