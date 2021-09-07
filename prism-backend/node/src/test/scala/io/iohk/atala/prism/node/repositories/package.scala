package io.iohk.atala.prism.node

import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.models.DIDSuffix
import io.iohk.atala.prism.node.models.CredentialId

package object repositories {

  def digestGen(kind: Byte, i: Byte): Sha256Digest =
    Sha256Digest.fromBytes(kind +: Array.fill(30)(0.toByte) :+ i)

  def didSuffixFromDigest(digest: Sha256Digest): DIDSuffix = DIDSuffix(digest.getHexValue)

  def credentialIdFromDigest(digest: Sha256Digest): CredentialId = CredentialId(digest.getHexValue)

}
