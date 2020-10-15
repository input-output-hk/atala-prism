package io.iohk.atala.prism.connector

import java.util.UUID

private[connector] case class RequestNonce(bytes: Array[Byte]) extends AnyVal {
  def +(bytes: Array[Byte]): Array[Byte] = {
    // Note: this.bytes ++ bytes requires implicit ClassTag, which causes errors in Android
    val result = new Array[Byte](this.bytes.length + bytes.length)
    this.bytes.copyToArray(result)
    bytes.copyToArray(result, this.bytes.length)
    result
  }
}

private[connector] object RequestNonce {
  def apply(): RequestNonce = {
    RequestNonce(bytes = UUID.randomUUID().toString.getBytes)
  }
}
