package io.iohk.atala.prism.identity

import java.util.Base64

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{ECConfig, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.protos.node_models

import scala.util.matching.Regex

final class DID private (val value: String) {
  import DID._

  def isLongForm: Boolean = {
    DIDFormat.longFormRegex.findFirstMatchIn(value).nonEmpty
  }

  def isCanonicalForm: Boolean = {
    DIDFormat.shortFormRegex.findFirstMatchIn(value).nonEmpty
  }

  def getFormat: DIDFormat = {
    if (isLongForm) {
      DIDFormat.LongForm(
        suffix.value.takeWhile(_ != ':'),
        suffix.value.dropWhile(_ != ':').tail
      )
    } else if (isCanonicalForm) {
      DIDFormat.Canonical(suffix.value.takeWhile(_ != ':'))
    } else {
      DIDFormat.Unknown
    }
  }

  // the method assumes that the DID is a PRISM DID
  def suffix: DIDSuffix = DIDSuffix.unsafeFromString(value.stripPrefix(prismPrefix))

  def getCanonicalSuffix: Option[DIDSuffix] = {
    getFormat match {
      case DIDFormat.Canonical(suffix) => Some(DIDSuffix.unsafeFromString(suffix))
      case DIDFormat.LongForm(stateHash, _) => Some(DIDSuffix.unsafeFromString(stateHash))
      case DIDFormat.Unknown => None
    }
  }

  def asLongForm: Option[DIDFormat.LongForm] = {
    getFormat match {
      case f: DIDFormat.LongForm => Some(f)
      case DIDFormat.Unknown | _: DIDFormat.Canonical => None
    }
  }

  def canonical: Option[DID] = {
    getCanonicalSuffix.map(buildPrismDID)
  }

  override def toString: String = value

  override def equals(other: Any): Boolean =
    other match {
      case that: DID =>
        value == that.value
      case _ => false
    }

  override def hashCode(): Int =
    value.hashCode
}

object DID {
  val suffixRegex: Regex = "[:A-Za-z0-9_-]+$".r
  val prismPrefix: String = "did:prism:"
  val prismRegex: Regex = "^did:prism(:[A-Za-z0-9_-]+)+$".r
  // This is the prefix we currently use in IntDemo TODO: Remove once possible
  val testRegex: Regex = "^did:test(:[A-Za-z0-9_-]+)+$".r

  private def apply(value: String): DID = new DID(value)

  def buildPrismDID(stateHash: String, maybeEncodedState: Option[String] = None): DID = {
    maybeEncodedState match {
      case None => DID(s"$prismPrefix$stateHash")
      case Some(encodedState) => buildPrismDID(stateHash = stateHash, encodedState = encodedState)
    }
  }

  def buildPrismDID(stateHash: String, encodedState: String): DID = {
    DID(s"$prismPrefix${buildSuffix(stateHash, encodedState)}")
  }

  def buildPrismDID(suffix: DIDSuffix): DID = {
    DID(s"$prismPrefix${suffix.value}")
  }

  def fromString(string: String): Option[DID] = {
    string match {
      case prismRegex(_*) | testRegex(_*) =>
        Some(DID(string))
      case _ =>
        None
    }
  }

  def unsafeFromString(string: String): DID = {
    fromString(string).getOrElse(throw new IllegalArgumentException(s"Invalid DID $string"))
  }

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

  def createUnpublishedDID(masterKey: ECPublicKey): DID = {
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
