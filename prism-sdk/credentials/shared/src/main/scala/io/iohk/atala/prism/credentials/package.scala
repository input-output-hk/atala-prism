package io.iohk.atala.prism

import java.nio.charset.StandardCharsets

package object credentials {

  private[credentials] val charsetUsed = StandardCharsets.UTF_8
  private[credentials] implicit class BytesOps(val bytes: Array[Byte]) {
    def asString: String = new String(bytes, charsetUsed)
  }
}
