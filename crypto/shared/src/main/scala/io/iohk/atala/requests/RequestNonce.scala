package io.iohk.atala.requests

import java.util.UUID

private[requests] case class RequestNonce(bytes: Array[Byte]) extends AnyVal {
  def +(bytes: Array[Byte]): Array[Byte] = {
    this.bytes ++ bytes
  }
}

private[requests] object RequestNonce {
  def apply(): RequestNonce = {
    RequestNonce(bytes = UUID.randomUUID().toString.getBytes)
  }
}
