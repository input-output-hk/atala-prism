package io.iohk.atala.prism.node.auth

import java.util.UUID

package object model {
  case class RequestNonce(bytes: Vector[Byte]) extends AnyVal {
    def mergeWith(request: Array[Byte]): Vector[Byte] = {
      this.bytes ++ request
    }
  }

  object RequestNonce {
    def random(): RequestNonce = {
      RequestNonce(bytes = UUID.randomUUID().toString.getBytes().toVector)
    }
  }

  case class AuthToken(token: String) extends AnyVal
}
