package io.iohk.cvp.grpc

object GrpcAuthenticationHeaderParser {

  /**
    * Get the authentication header from the current context.
    *
    * NOTE: This method is unsafe as it uses the thread local to get the current context, call it immediately after
    *       receiving a gRPC request, if this gets called in a different ExecutionContext (when dealing with Futures),
    *       the header won't be the same that was sent on the gRPC request.
    */
  def current(): Option[GrpcAuthenticationHeader] = {
    GrpcAuthenticationContext
      .parseDIDAuthenticationHeader()
      .orElse(GrpcAuthenticationContext.parsePublicKeyAuthenticationHeader())
      .orElse(GrpcAuthenticationContext.parseLegacyAuthenticationContext())
  }
}
