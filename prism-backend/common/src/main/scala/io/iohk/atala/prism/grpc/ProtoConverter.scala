package io.iohk.atala.prism.grpc

import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import scalapb.GeneratedMessage

import scala.util.Try

trait ProtoConverter[P <: GeneratedMessage, T] {
  def fromProto(proto: P, grpcHeader: Option[GrpcAuthenticationHeader]): Try[T]
}

object ProtoConverter {
  implicit def anyToUnitConverter[T <: GeneratedMessage]: ProtoConverter[T, Unit] = (_, _) => Try(())

  def apply[P <: GeneratedMessage, T](implicit
      pc: ProtoConverter[P, T]
  ): ProtoConverter[P, T] = pc
}
