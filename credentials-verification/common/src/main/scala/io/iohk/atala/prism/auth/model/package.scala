package io.iohk.atala.prism.auth

package object model {
  case class RequestNonce(bytes: Vector[Byte]) extends AnyVal
}
