package io.iohk.atala.cvp.webextension.common.models

import java.util.UUID

case class RequestNonce(bytes: Array[Byte]) extends AnyVal {
  def +(bytes: Array[Byte]): Array[Byte] = {
    this.bytes ++ bytes
  }
}

object RequestNonce {
  def apply(): RequestNonce = {
    RequestNonce(bytes = UUID.randomUUID().toString.getBytes)
  }
}
