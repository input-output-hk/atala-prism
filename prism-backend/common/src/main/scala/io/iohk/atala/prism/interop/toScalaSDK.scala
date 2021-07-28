package io.iohk.atala.prism.interop
import com.google.protobuf.ByteString
import io.iohk.atala.prism.kotlin.crypto.keys.{ECKeyPair, ECPrivateKey, ECPublicKey}
import io.iohk.atala.prism.crypto.{
  JvmECPrivateKey,
  JvmECPublicKey,
  ECKeyPair => ECKeyPairScalaSDK,
  ECPrivateKey => ECPrivateKeyScalaSDK,
  ECPublicKey => ECPublicKeyScalaSDK
}
import io.iohk.atala.prism.kotlin.crypto.{MerkleInclusionProof, MerkleRoot, SHA256Digest}
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleRoot => MerkleRootScalaSDK}
import io.iohk.atala.prism.crypto.{SHA256Digest => SHA256DigestScalaSDK}
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof => MerkleInclusionProofScalaSDK}
import io.iohk.atala.prism.kotlin.protos.{DIDData, KeyUsage, PublicKey}
import io.iohk.atala.prism.protos.node_models.{DIDData => DIDDAtaScalaSDK}

import scala.jdk.CollectionConverters._

object toScalaSDK {
  implicit class ECPublicKeyInterop(private val v: ECPublicKey) extends AnyVal {
    def asScala: ECPublicKeyScalaSDK = {

      new JvmECPublicKey(v.getKey$prism_crypto)
    }
  }

  implicit class ECPrivateKeyInterop(private val v: ECPrivateKey) extends AnyVal {
    def asScala: ECPrivateKeyScalaSDK = {

      new JvmECPrivateKey(v.getKey$prism_crypto)
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

  implicit class DIDDataInterop(private val v: DIDData) extends AnyVal {
    def asScala: DIDDAtaScalaSDK = {
      DIDDAtaScalaSDK(v.getId, v.getPublicKeys.asScala.toList.map(_.asScala))
    }
  }

  implicit class ProtosPublicKeyInterop(private val v: io.iohk.atala.prism.kotlin.protos.PublicKey) extends AnyVal {
    def asScala: io.iohk.atala.prism.protos.node_models.PublicKey = {
      io.iohk.atala.prism.protos.node_models
        .PublicKey(
          v.getId,
          v.getUsage.asScala,
          Option(v.getAddedOn.asScala),
          Option(v.getRevokedOn.asScala),
          v.getKeyData.asScala
        )
    }
  }

  implicit class ProtosKeyDataInterop(private val v: io.iohk.atala.prism.kotlin.protos.PublicKey.KeyData[_])
      extends AnyVal {
    def asScala: io.iohk.atala.prism.protos.node_models.PublicKey.KeyData = {
      v match {
        case data: PublicKey.KeyData.EcKeyData =>
          io.iohk.atala.prism.protos.node_models.PublicKey.KeyData.EcKeyData(
            io.iohk.atala.prism.protos.node_models.ECKeyData(
              data.getValue.getCurve,
              ByteString.copyFrom(data.getValue.getX.getArray),
              ByteString.copyFrom(data.getValue.getY.getArray)
            )
          )
        case _ => io.iohk.atala.prism.protos.node_models.PublicKey.KeyData.Empty
      }
    }
  }

  implicit class ProtosTimestampInfoInterop(private val v: io.iohk.atala.prism.kotlin.protos.TimestampInfo)
      extends AnyVal {
    def asScala: io.iohk.atala.prism.protos.node_models.TimestampInfo = {
      io.iohk.atala.prism.protos.node_models
        .TimestampInfo(v.getBlockSequenceNumber, v.getOperationSequenceNumber, Option(v.getBlockTimestamp.asScala))
    }
  }

  implicit class ProtosTimestampInterop(private val v: pbandk.wkt.Timestamp) extends AnyVal {
    def asScala: com.google.protobuf.timestamp.Timestamp = {
      com.google.protobuf.timestamp.Timestamp(v.getSeconds, v.getNanos)
    }
  }

  implicit class ProtosKeyUsageInterop(private val v: io.iohk.atala.prism.kotlin.protos.KeyUsage) extends AnyVal {
    def asScala: io.iohk.atala.prism.protos.node_models.KeyUsage = {
      v match {
        case _: KeyUsage.UNKNOWN_KEY => io.iohk.atala.prism.protos.node_models.KeyUsage.UNKNOWN_KEY
        case _: KeyUsage.MASTER_KEY => io.iohk.atala.prism.protos.node_models.KeyUsage.MASTER_KEY
        case _: KeyUsage.AUTHENTICATION_KEY =>
          io.iohk.atala.prism.protos.node_models.KeyUsage.AUTHENTICATION_KEY
        case _: KeyUsage.ISSUING_KEY => io.iohk.atala.prism.protos.node_models.KeyUsage.ISSUING_KEY
        case unrecognized: KeyUsage.UNRECOGNIZED =>
          io.iohk.atala.prism.protos.node_models.KeyUsage.Unrecognized(unrecognized.getValue)
        case _: KeyUsage.COMMUNICATION_KEY =>
          io.iohk.atala.prism.protos.node_models.KeyUsage.COMMUNICATION_KEY
      }
    }
  }
}
