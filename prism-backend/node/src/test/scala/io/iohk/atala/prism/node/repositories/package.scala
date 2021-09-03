package io.iohk.atala.prism.node

import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.node.models.CredentialId

package object repositories {

  def digestGen(kind: Byte, i: Byte): Sha256Digest =
    Sha256Digest.fromBytes(kind.toByte +: Array.fill(30)(0.toByte) :+ i.toByte)

  def didSuffixFromDigest(digest: Sha256Digest): String = digest.getHexValue

  def credentialIdFromDigest(digest: Sha256Digest): CredentialId = CredentialId(digest.getHexValue)

}
