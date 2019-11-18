package io.iohk.node

import io.iohk.node.models.{CredentialId, DIDSuffix, SHA256Digest}

package object repositories {

  def digestGen(kind: Byte, i: Byte) = SHA256Digest(kind.toByte +: Array.fill(30)(0.toByte) :+ i.toByte)

  def didSuffixFromDigest(digest: SHA256Digest) = DIDSuffix(digest.hexValue)

  def credentialIdFromDigest(digest: SHA256Digest) = CredentialId(digest.hexValue)

}
