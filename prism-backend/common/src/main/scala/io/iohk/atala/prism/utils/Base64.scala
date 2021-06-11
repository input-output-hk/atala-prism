package io.iohk.atala.prism.utils

import java.util.Base64

object Base64Utils {
  def encodeURL(bytes: Array[Byte]): String = {
    Base64.getUrlEncoder.encodeToString(bytes)
  }
}
