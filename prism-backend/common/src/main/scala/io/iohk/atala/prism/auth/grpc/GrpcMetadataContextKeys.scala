package io.iohk.atala.prism.auth.grpc

import io.grpc.{Context, Metadata}

/** Usually, we need to define the Metadata and the Context keys together, this class makes it easier to do that.
  */
case class GrpcMetadataContextKeys[T](
    metadata: Metadata.Key[String],
    context: Context.Key[T]
)

object GrpcMetadataContextKeys {
  def apply[T](name: String): GrpcMetadataContextKeys[T] = {
    val metadata = Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER)
    val context = Context.key[T](name)
    GrpcMetadataContextKeys(metadata, context)
  }
}
