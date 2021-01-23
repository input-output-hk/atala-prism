package io.iohk.atala.prism.models

import java.util.UUID
import scala.util.Try

trait UUIDValue extends Any {
  def uuid: UUID

  override def toString: String = uuid.toString
}

object UUIDValue {
  abstract class Builder[A <: UUIDValue] {
    def apply(uuid: UUID): A

    def random(): A = {
      apply(UUID.randomUUID())
    }

    def from(string: String): Try[A] =
      Try {
        apply(UUID.fromString(string))
      }

    def unsafeFrom(string: String): A = {
      from(string).getOrElse(throw new IllegalArgumentException(s"`$string` is not a valid UUID"))
    }
  }
}
