package io.iohk.atala.prism.interop
import io.iohk.atala.prism.kotlin.crypto.keys.{ECKeyPair, ECPrivateKey, ECPublicKey}
import io.iohk.atala.prism.crypto.{
  JvmECPrivateKey,
  JvmECPublicKey,
  ECPrivateKey => ECPrivateKeyScalaSDK,
  ECPublicKey => ECPublicKeyScalaSDK,
  ECKeyPair => ECKeyPairScalaSDK
}
import io.iohk.atala.prism.kotlin.crypto.{MerkleInclusionProof, MerkleRoot, SHA256Digest}
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleRoot => MerkleRootScalaSDK}
import io.iohk.atala.prism.crypto.{SHA256Digest => SHA256DigestScalaSDK}
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof => MerkleInclusionProofScalaSDK}

import scala.jdk.CollectionConverters._

object toScalaSDK {
  implicit class ECPublicKeyInterop(private val v: ECPublicKey) extends AnyVal {
    def asScala: ECPublicKeyScalaSDK = {

      new JvmECPublicKey(v.getKey$crypto)
    }
  }

  implicit class ECPrivateKeyInterop(private val v: ECPrivateKey) extends AnyVal {
    def asScala: ECPrivateKeyScalaSDK = {

      new JvmECPrivateKey(v.getKey$crypto)
    }
  }

  implicit class SHA256DigestInterop(private val v: SHA256Digest) extends AnyVal {
    def asScala: SHA256DigestScalaSDK = {

      SHA256DigestScalaSDK.compute(v.getValue)
    }
  }

  implicit class ECKeyPairInterop(private val v: ECKeyPair) extends AnyVal {
    def asScala: ECKeyPairScalaSDK = {
      ECKeyPairScalaSDK(v.getPrivateKey.asScala, v.getPublicKey.asScala)

    }
  }

  implicit class MerkleRootInterop(private val v: MerkleRoot) extends AnyVal {
    def asScala: MerkleRootScalaSDK = {

      MerkleRootScalaSDK(v.getHash.asScala)
    }
  }

  implicit class MerkleInclusionProofInterop(private val v: MerkleInclusionProof) extends AnyVal {
    def asScala: MerkleInclusionProofScalaSDK = {

      MerkleInclusionProofScalaSDK(v.getHash.asScala, v.getIndex, v.getSiblings.asScala.toList.map(_.asScala))
    }
  }
}
