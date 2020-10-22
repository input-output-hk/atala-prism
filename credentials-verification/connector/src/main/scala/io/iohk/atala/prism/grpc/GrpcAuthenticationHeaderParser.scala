package io.iohk.atala.prism.grpc

import io.grpc.Context

trait GrpcAuthenticationHeaderParser {

  /**
    * Get the authentication header from the current context.
    */
  def parse(ctx: Context): Option[GrpcAuthenticationHeader] = {
    GrpcAuthenticationContext
      .parseDIDAuthenticationHeader(ctx)
      .orElse(GrpcAuthenticationContext.parsePublicKeyAuthenticationHeader(ctx))
      .orElse(GrpcAuthenticationContext.parseLegacyAuthenticationContext(ctx))
  }
}

object GrpcAuthenticationHeaderParser extends GrpcAuthenticationHeaderParser
