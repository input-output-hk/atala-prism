package io.iohk.atala.prism.grpc

import scalapb.GeneratedMessage

import scala.util.Try

trait ProtoConverter[P <: GeneratedMessage, T] {
  def fromProto(proto: P): Try[T]
}

object ProtoConverter {
  implicit def anyToUnitConverter[T <: GeneratedMessage]: ProtoConverter[T, Unit] = _ => Try(())

}
