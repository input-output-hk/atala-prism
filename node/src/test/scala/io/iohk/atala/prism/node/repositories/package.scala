package io.iohk.atala.prism.node

import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.node.models.DidSuffix

package object repositories {

  def digestGen(kind: Byte, i: Byte): Sha256Digest =
    Sha256Digest.fromBytes(kind +: Array.fill(30)(0.toByte) :+ i)

  def didSuffixFromDigest(digest: Sha256Digest): DidSuffix = DidSuffix(
    digest.getHexValue
  )

}
