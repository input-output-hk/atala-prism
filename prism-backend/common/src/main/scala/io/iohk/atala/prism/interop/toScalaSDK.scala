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
import io.iohk.atala.prism.kotlin.protos.{AtalaOperation, DIDData, KeyUsage, PublicKey, UpdateDIDAction}
import io.iohk.atala.prism.protos.node_models
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
    def asScala: node_models.PublicKey = {
      io.iohk.atala.prism.protos.node_models
        .PublicKey(
          id = v.getId,
          usage = v.getUsage.asScala,
          addedOn = Option(v.getAddedOn.getTimestampInfo).map(_.asScala),
          revokedOn = Option(v.getRevokedOn.getTimestampInfo).map(_.asScala),
          keyData = v.getKeyData.asScala
        )
    }
  }

  implicit class AtalaOperationInterop(private val v: io.iohk.atala.prism.kotlin.protos.AtalaOperation) extends AnyVal {
    def asScala: node_models.AtalaOperation = {
      val op = v.getOperation match {
        case op: AtalaOperation.Operation.CreateDid =>
          node_models.AtalaOperation.Operation.CreateDid(op.getValue.asScala)
        case op: AtalaOperation.Operation.IssueCredentialBatch =>
          node_models.AtalaOperation.Operation.IssueCredentialBatch(op.getValue.asScala)
        case op: AtalaOperation.Operation.RevokeCredentials =>
          node_models.AtalaOperation.Operation.RevokeCredentials(op.getValue.asScala)
        case op: AtalaOperation.Operation.UpdateDid =>
          node_models.AtalaOperation.Operation.UpdateDid(op.getValue.asScala)
        case _ => node_models.AtalaOperation.Operation.Empty
      }

      node_models.AtalaOperation(op)
    }
  }

  implicit class UpdateDIDOperationInterop(private val v: io.iohk.atala.prism.kotlin.protos.UpdateDIDOperation)
      extends AnyVal {
    def asScala: node_models.UpdateDIDOperation =
      node_models.UpdateDIDOperation(ByteString.copyFrom(v.getPreviousOperationHash.getArray), v.getId)
  }

  implicit class IssueCredentialBatchInterop(
      private val v: io.iohk.atala.prism.kotlin.protos.IssueCredentialBatchOperation
  ) extends AnyVal {
    def asScala: node_models.IssueCredentialBatchOperation =
      node_models.IssueCredentialBatchOperation(Option(v.getCredentialBatchData).map(_.asScala))
  }

  implicit class UpdateDIDActionInterop(private val v: io.iohk.atala.prism.kotlin.protos.UpdateDIDAction)
      extends AnyVal {
    def asScala: node_models.UpdateDIDAction.Action =
      v.getAction match {
        case key: UpdateDIDAction.Action.AddKey => node_models.UpdateDIDAction.Action.AddKey(key.getValue.asScala)
        case key: UpdateDIDAction.Action.RemoveKey => node_models.UpdateDIDAction.Action.RemoveKey(key.getValue.asScala)
        case _ => node_models.UpdateDIDAction.Action.Empty
      }
  }

  implicit class AddKeyActionInterop(private val v: io.iohk.atala.prism.kotlin.protos.AddKeyAction) extends AnyVal {
    def asScala: node_models.AddKeyAction = node_models.AddKeyAction(key = Option(v.getKey).map(_.asScala))
  }

  implicit class RemoveKeyActionInterop(private val v: io.iohk.atala.prism.kotlin.protos.RemoveKeyAction)
      extends AnyVal {
    def asScala: node_models.RemoveKeyAction = node_models.RemoveKeyAction(keyId = v.getKeyId)
  }

  implicit class RevokeCredentialsOperationInterop(
      private val v: io.iohk.atala.prism.kotlin.protos.RevokeCredentialsOperation
  ) extends AnyVal {
    def asScala: node_models.RevokeCredentialsOperation =
      node_models.RevokeCredentialsOperation(
        ByteString.copyFrom(v.getPreviousOperationHash.getArray),
        v.getCredentialBatchId,
        v.getCredentialsToRevoke.asScala.map(arr => ByteString.copyFrom(arr.getArray)).toList
      )
  }

  implicit class CredentialBatchDataInterop(private val v: io.iohk.atala.prism.kotlin.protos.CredentialBatchData)
      extends AnyVal {
    def asScala: node_models.CredentialBatchData =
      node_models.CredentialBatchData(v.getIssuerDid, ByteString.copyFrom(v.getMerkleRoot.getArray))
  }

  implicit class CreateDIDOperationInterop(private val v: io.iohk.atala.prism.kotlin.protos.CreateDIDOperation)
      extends AnyVal {
    def asScala: node_models.CreateDIDOperation = node_models.CreateDIDOperation(Option(v.getDidData).map(_.asScala))
  }

  implicit class ProtosKeyDataInterop(private val v: io.iohk.atala.prism.kotlin.protos.PublicKey.KeyData[_])
      extends AnyVal {
    def asScala: node_models.PublicKey.KeyData = {
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
    def asScala: node_models.TimestampInfo = {
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
    def asScala: node_models.KeyUsage = {
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
