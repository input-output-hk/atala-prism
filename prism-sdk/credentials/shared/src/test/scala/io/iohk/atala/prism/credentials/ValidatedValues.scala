package io.iohk.atala.prism.credentials

import cats.data.Validated
import org.scalactic.source
import org.scalatest.exceptions.{StackDepthException, TestFailedException}

trait ValidatedValues {
  implicit def convertValidatedToOps[E, A](validated: Validated[E, A])(implicit
      pos: source.Position
  ): ValidatedOps[E, A] = new ValidatedOps(validated, pos)

  class ValidatedOps[E, A](validated: Validated[E, A], pos: source.Position) {
    def valid: A = {
      validated match {
        case Validated.Valid(a) =>
          a
        case Validated.Invalid(e) =>
          throw new TestFailedException(
            (_: StackDepthException) => Some("Expected a valid value, but got " + e),
            None,
            pos
          )
      }
    }

    def invalid: E = {
      validated match {
        case Validated.Valid(a) =>
          throw new TestFailedException(
            (_: StackDepthException) => Some("Expected an invalid value, but got " + a),
            None,
            pos
          )
        case Validated.Invalid(e) =>
          e
      }
    }
  }
}
