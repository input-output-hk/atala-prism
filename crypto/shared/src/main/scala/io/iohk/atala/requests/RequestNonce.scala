package io.iohk.atala.requests

import java.util.UUID

private[requests] case class RequestNonce(bytes: Array[Byte]) extends AnyVal {
  def +(bytes: Array[Byte]): Array[Byte] = {
    // Note: this.bytes ++ bytes requires implicit ClassTag, which causes errors in Android
    val result = new Array[Byte](this.bytes.length + bytes.length)
    this.bytes.copyToArray(result)
    bytes.copyToArray(result, this.bytes.length)
    result
  }
}

private[requests] object RequestNonce {
  def apply(): RequestNonce = {
    RequestNonce(bytes = UUID.randomUUID().toString.getBytes)
  }
}
