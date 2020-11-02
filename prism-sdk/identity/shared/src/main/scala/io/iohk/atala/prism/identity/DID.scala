package io.iohk.atala.prism.identity

import java.util.Base64

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{ECConfig, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.protos.node_models

import scala.util.matching.Regex

object DID {
  val prismPrefix: String = "did:prism:"
  def buildPrismDID(stateHash: String, maybeEncodedState: Option[String] = None): String = {
    maybeEncodedState match {
      case None => s"$prismPrefix$stateHash"
      case Some(encodedState) => s"$prismPrefix${buildSuffix(stateHash, encodedState)}"
    }
  }

  def buildPrismDID(stateHash: String, encodedState: String): String =
    s"$prismPrefix${buildSuffix(stateHash, encodedState)}"

  private def buildSuffix(stateHash: String, encodedState: String): String = s"$stateHash:$encodedState"

  case class ValidatedLongForm(stateHash: String, encodedState: String, initialState: node_models.AtalaOperation) {
    def suffix: String = buildSuffix(stateHash, encodedState)
  }

  sealed trait DIDFormat
  object DIDFormat {
    val longFormRegex: Regex = "^did:prism:[0-9a-f]{64}:[A-Za-z0-9_-]+[=]*$".r
    val shortFormRegex: Regex = "^did:prism:[0-9a-f]{64}$".r

    sealed trait DIDFormatError
    sealed trait DIDLongFormError extends DIDFormatError
    case object CanonicalSuffixMatchStateError extends DIDLongFormError
    case object InvalidAtalaOperationError extends DIDLongFormError

    case class Canonical(suffix: String) extends DIDFormat
    case class LongForm(stateHash: String, encodedState: String) extends DIDFormat {
      def validate: Either[DIDLongFormError, ValidatedLongForm] = {
        val atalaOperationBytes = Base64.getUrlDecoder.decode(encodedState)
        if (stateHash == SHA256Digest.compute(atalaOperationBytes).hexValue) {
          node_models.AtalaOperation
            .validate(atalaOperationBytes)
            .toOption
            .toRight(InvalidAtalaOperationError)
            .map(ValidatedLongForm(stateHash, encodedState, _))
        } else {
          Left(CanonicalSuffixMatchStateError)
        }
      }

      def getInitialState: Either[DIDLongFormError, node_models.AtalaOperation] =
        validate.map(_.initialState)
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
        stripPrismPrefix(did).takeWhile(_ != ':'),
        stripPrismPrefix(did).dropWhile(_ != ':').tail
      )
    } else if (isCanonicalForm(did)) {
      DIDFormat.Canonical(stripPrismPrefix(did).takeWhile(_ != ':'))
    } else {
      DIDFormat.Unknown
    }
  }

  def stripPrismPrefix(did: String): String = did.stripPrefix(prismPrefix)

  def getSuffix(did: String): Option[String] = {
    getFormat(did) match {
      case DIDFormat.Canonical(suffix) => Some(suffix)
      case DIDFormat.LongForm(stateHash, encodedState) => Some(buildSuffix(stateHash, encodedState))
      case DIDFormat.Unknown => None
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
    // DID method-specific id must consist out of alpha-digit characters plus '.', '-' and '_'.
    // Hence we are using URL-safe Base64 encoder without padding (i.e. no trailing '='s).
    val encodedOperation = Base64.getUrlEncoder.withoutPadding().encodeToString(operationBytes)
    buildPrismDID(didCanonicalSuffix, encodedOperation)
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
