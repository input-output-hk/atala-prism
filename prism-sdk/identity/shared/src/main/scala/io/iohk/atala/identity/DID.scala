package io.iohk.atala.identity

import java.util.Base64

import com.google.protobuf.ByteString
import io.iohk.atala.crypto.{ECConfig, ECPublicKey, SHA256Digest}
import io.iohk.prism.protos.node_models

import scala.util.matching.Regex

object DID {

  case class ValidatedLongForm(stateHash: String, encodedState: String, initialState: node_models.AtalaOperation)

  sealed trait DIDFormat
  object DIDFormat {
    val longFormRegex: Regex = "^did:prism:[0-9a-f]{64}:[A-Za-z0-9_-]+$".r
    val shortFormRegex: Regex = "^did:prism:[0-9a-f]{64}$".r

    case class Canonical(suffix: String) extends DIDFormat
    case class LongForm(stateHash: String, encodedState: String) extends DIDFormat {
      def validate: Option[ValidatedLongForm] = {
        val atalaOperationBytes = Base64.getUrlDecoder.decode(encodedState)
        if (stateHash == SHA256Digest.compute(atalaOperationBytes).hexValue) {
          node_models.AtalaOperation
            .validate(atalaOperationBytes)
            .toOption
            .map(ValidatedLongForm(stateHash, encodedState, _))
        } else {
          None
        }
      }

      def getInitialState: Option[node_models.AtalaOperation] = validate.map(_.initialState)
    }
    case object Unknown extends DIDFormat
  }

  def isLongForm(did: String): Boolean = {
    DIDFormat.longFormRegex.findFirstMatchIn(did).nonEmpty
  }

  def isCanonicalForm(did: String): Boolean = {
    DIDFormat.shortFormRegex.findFirstMatchIn(did).nonEmpty
  }

  def getFormat(did: String): DIDFormat = {
    if (isLongForm(did)) {
      DIDFormat.LongForm(
        did.stripPrefix("did:prism:").takeWhile(_ != ':'),
        did.stripPrefix("did:prism:").dropWhile(_ != ':').tail
      )
    } else if (isCanonicalForm(did)) {
      DIDFormat.Canonical(did.stripPrefix("did:prism:").takeWhile(_ != ':'))
    } else {
      DIDFormat.Unknown
    }
  }

  def getCanonicalSuffix(did: String): Option[String] = {
    getFormat(did) match {
      case DIDFormat.Canonical(suffix) => Some(suffix)
      case DIDFormat.LongForm(stateHash, _) => Some(stateHash)
      case DIDFormat.Unknown => None
    }
  }

  def createUnpublishedDID(masterKey: ECPublicKey): String = {
    val createDidOp = node_models.CreateDIDOperation(
      didData = Some(
        node_models.DIDData(
          publicKeys = Seq(
            node_models.PublicKey(
              id = s"master0",
              usage = node_models.KeyUsage.MASTER_KEY,
              keyData = node_models.PublicKey.KeyData.EcKeyData(
                publicKeyToProto(masterKey)
              )
            )
          )
        )
      )
    )

    val atalaOp = node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))
    val operationBytes = atalaOp.toByteArray
    val operationHash = SHA256Digest.compute(operationBytes)
    val didCanonicalSuffix = operationHash.hexValue
    val encodedOperation = Base64.getUrlEncoder.encodeToString(operationBytes)
    s"did:prism:$didCanonicalSuffix:$encodedOperation"
  }

  private[identity] def publicKeyToProto(key: ECPublicKey): node_models.ECKeyData = {
    val point = key.getCurvePoint
    node_models.ECKeyData(
      curve = ECConfig.CURVE_NAME,
      x = ByteString.copyFrom(point.x.toByteArray),
      y = ByteString.copyFrom(point.y.toByteArray)
    )
  }
}
