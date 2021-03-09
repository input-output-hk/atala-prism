package io.iohk.atala.prism.node

import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.node.models.CredentialId

package object repositories {

  def digestGen(kind: Byte, i: Byte): SHA256Digest =
    SHA256Digest.fromVectorUnsafe(kind.toByte +: Vector.fill(30)(0.toByte) :+ i.toByte)

  def didSuffixFromDigest(digest: SHA256Digest): DIDSuffix = DIDSuffix.unsafeFromDigest(digest)

  def credentialIdFromDigest(digest: SHA256Digest): CredentialId = CredentialId(digest.hexValue)

}
