package io.iohk.atala.prism.node

import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DIDSuffix
import io.iohk.atala.prism.node.models.CredentialId

import io.iohk.atala.prism.interop.toScalaSDK._

package object repositories {

  def digestGen(kind: Byte, i: Byte): SHA256Digest =
    SHA256Digest.fromBytes(kind.toByte +: Array.fill(30)(0.toByte) :+ i.toByte)

  def didSuffixFromDigest(digest: SHA256Digest): DIDSuffix = DIDSuffix.fromDigest(digest)

  def credentialIdFromDigest(digest: SHA256Digest): CredentialId = CredentialId(digest.hexValue)

}
