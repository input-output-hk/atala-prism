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

    def from(string: String): Try[A] = {
      Try {
        apply(UUID.fromString(string))
      }
    }

    def optional(string: String): Try[Option[A]] = {
      Option(string)
        .filter(_.nonEmpty)
        .map { string =>
          Try(UUID.fromString(string))
            .map(apply)
            .map(Option.apply)
        }
        .getOrElse(Try(None))
    }

    def unsafeFrom(string: String): A = {
      from(string).getOrElse(
        throw new IllegalArgumentException(s"`$string` is not a valid UUID")
      )
    }
  }
}
