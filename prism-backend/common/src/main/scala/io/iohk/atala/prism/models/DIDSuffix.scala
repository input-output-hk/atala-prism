package io.iohk.atala.prism.models

import io.iohk.atala.prism.kotlin.crypto.Sha256Digest

case class DIDSuffix(value: String) extends AnyVal {
  def getValue: String = value
}

object DIDSuffix {
  def fromDigest(in: Sha256Digest): DIDSuffix = DIDSuffix(in.getHexValue)
}
