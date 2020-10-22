package io.iohk.atala.prism.node

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.node.models.{CredentialId, DIDSuffix}

package object repositories {

  def digestGen(kind: Byte, i: Byte) = SHA256Digest(kind.toByte +: Vector.fill(30)(0.toByte) :+ i.toByte)

  def didSuffixFromDigest(digest: SHA256Digest) = DIDSuffix(digest.hexValue)

  def credentialIdFromDigest(digest: SHA256Digest) = CredentialId(digest.hexValue)

}
