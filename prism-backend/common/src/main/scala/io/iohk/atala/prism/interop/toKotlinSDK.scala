package io.iohk.atala.prism.interop

import io.iohk.atala.prism.crypto.JvmECPublicKey
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.DIDSuffix

import scala.jdk.CollectionConverters._

object toKotlinSDK {
  implicit class MerkleRootScalaSDKInterop(v: io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot) {
    def asKotlin: MerkleRoot = {

      new MerkleRoot(v.hash.asKotlin)
    }
  }

  implicit class MerkleInclusionProofScalaSDKInterop(v: io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof) {
    def asKotlin: MerkleInclusionProof = {

      new MerkleInclusionProof(v.hash.asKotlin, v.index, v.siblings.map(_.asKotlin).asJava)
    }
  }

  implicit class SHA256DigestScalaSDKInterop(v: io.iohk.atala.prism.crypto.SHA256Digest) {
    def asKotlin: SHA256Digest = {

      new SHA256Digest(v.value.toArray)
    }
  }

  implicit class ECPublicKeyScalaSDKInterop(v: io.iohk.atala.prism.crypto.ECPublicKey) {
    def asKotlin: ECPublicKey =
      v match {
        case key: JvmECPublicKey => new ECPublicKey(key.key)
      }
  }
  implicit class ECPrivateKeyScalaSDKInterop(v: io.iohk.atala.prism.crypto.ECPrivateKey) {
    def asKotlin: ECPrivateKey =
      v match {
        case key: JvmECPrivateKey => new ECPrivateKey(key.key)
      }
  }

  implicit class DIDSuffixScalaSDKInterop(v: io.iohk.atala.prism.identity.DIDSuffix) {
    def asKotlin: DIDSuffix = new DIDSuffix(v.value)
  }
}
