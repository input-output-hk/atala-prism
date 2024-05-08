package io.iohk.atala.prism.node

import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models.DidSuffix

package object repositories {

  def digestGen(kind: Byte, i: Byte): Sha256Hash =
    Sha256Hash.fromBytes(kind +: Array.fill(30)(0.toByte) :+ i)

  def didSuffixFromDigest(digest: Sha256Hash): DidSuffix = DidSuffix(
    digest.hexEncoded
  )

}
