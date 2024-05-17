package io.iohk.atala.prism.node.identity

import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPublicKey, Sha256Hash}
import io.iohk.atala.prism.protos.node_models.AtalaOperation.Operation
import io.iohk.atala.prism.protos.node_models._
import io.iohk.atala.prism.node.utils.Base64Utils
import io.iohk.atala.prism.node.models.{KeyUsage => KeyUsageModel}

sealed trait PrismDid {

  val did: Did
  val stateHash: Sha256Hash

  val value: String = did.toString()

  val suffix: String = did.methodSpecificId.toString()

  def asCanonical(): CanonicalPrismDid
}

object PrismDid {
  private val CANONICAL_SUFFIX_LENGTH = 64
  private val CANONICAL_SUFFIX_REGEX = "^([0-9a-f]{64}$)".r
  private val LONG_FORM_SUFFIX_REGEX = "^([0-9a-f]{64}):([A-Za-z0-9_-]+$)".r

  val PRISM_METHOD: DidMethod = DidMethod("prism")

  val DEFAULT_MASTER_KEY_ID: String = "master0"

  val DEFAULT_ISSUING_KEY_ID: String = "issuing0"

  val DEFAULT_KEY_AGREEMENT_KEY_ID: String = "keyAgreement0"

  val DEFAULT_AUTHENTICATION_KEY_ID: String = "authentication0"

  val DEFAULT_REVOCATION_KEY_ID: String = "revocation0"

  val DEFAULT_CAPABILITY_INVOCATION_KEY_ID: String = "capabilityInvocation0"

  val DEFAULT_CAPABILITY_DELEGATION_KEY_ID: String = "capabilityDelegation0"

  private def decodeState(stateHash: Sha256Hash, encodedState: Array[Byte]): AtalaOperation = {
    if (stateHash == Sha256Hash.compute(encodedState)) {
      val operation =
        try {
          AtalaOperation.parseFrom(encodedState)
        } catch {
          case e: Exception => throw InvalidAtalaOperationException(e)
        }
      operation
    } else {
      throw CanonicalSuffixMatchStateException
    }
  }

  private def longFormDidFromAtalaOperation(
      did: Did,
      stateHash: Sha256Hash,
      atalaOperation: AtalaOperation
  ): LongFormPrismDid = {
    val op = atalaOperation.operation
    op match {
      case Operation.CreateDid(value) =>
        val exists =
          value.didData.getOrElse(throw new NullPointerException()).publicKeys.exists(_.usage == KeyUsage.MASTER_KEY)

        require(exists, "At least one public key with master role required")

        LongFormPrismDid(did, stateHash, atalaOperation)

      case _ => throw CreateDidExpectedAsInitialState(op)
    }
  }

  private def createDidAtalaOperation(
      didPublicKeys: List[DidPublicKey]
  ): AtalaOperation = {
    AtalaOperation(
      AtalaOperation.Operation.CreateDid(
        CreateDIDOperation(
          didData = Some(
            CreateDIDOperation.DIDCreationData(
              publicKeys = didPublicKeys.map(_.toProto)
            )
          )
        )
      )
    )
  }

  private def createDidAtalaOperationFromMasterPublicKey(
      masterKey: SecpPublicKey
  ): AtalaOperation =
    createDidAtalaOperation(List(DidPublicKey(DEFAULT_MASTER_KEY_ID, KeyUsageModel.MasterKey, masterKey)))

  private def createExperimentalDidAtalaOperation(
      masterKey: SecpPublicKey,
      issuingKey: SecpPublicKey,
      revocationKey: SecpPublicKey
  ): AtalaOperation =
    createDidAtalaOperation(
      List(
        DidPublicKey(DEFAULT_MASTER_KEY_ID, KeyUsageModel.MasterKey, masterKey),
        DidPublicKey(DEFAULT_ISSUING_KEY_ID, KeyUsageModel.IssuingKey, issuingKey),
        DidPublicKey(DEFAULT_REVOCATION_KEY_ID, KeyUsageModel.RevocationKey, revocationKey)
      )
    )

  def fromDid(did: Did): PrismDid = {
    require(
      did.method == PRISM_METHOD,
      s"""Expected DID to have method "$PRISM_METHOD", but got "${did.method}" instead"""
    )

    val canonicalMatchGroups =
      CANONICAL_SUFFIX_REGEX.findFirstMatchIn(did.methodSpecificId.toString).map(x => x.subgroups)
    val longFormMatchGroups =
      LONG_FORM_SUFFIX_REGEX.findFirstMatchIn(did.methodSpecificId.toString).map(x => x.subgroups)

    canonicalMatchGroups match {
      case Some(groups) =>
        require(groups.size == 1, "Invalid canonical form Prism DID")
        CanonicalPrismDid(did, Sha256Hash.fromHex(groups.head))

      case None =>
        longFormMatchGroups match {
          case Some(groups) =>
            val List(stateHashHex, encodedStateBase64, _*) = groups
            require(groups.size == 2, "Invalid long form Prism DID")
            val stateHash = Sha256Hash.fromHex(stateHashHex)
            val encodedState = Base64Utils.decodeURL(encodedStateBase64)
            val atalaOperation = decodeState(stateHash, encodedState)
            longFormDidFromAtalaOperation(did, stateHash, atalaOperation)
          case None => throw UnrecognizedPrismDidException
        }
    }
  }

  def fromString(string: String): PrismDid = {
    fromDid(Did.fromString(string))
  }

  def canonicalFromString(string: String): CanonicalPrismDid = {
    // preliminary did length check to avoid costly computations
    val expectedLength = Did.DID_SCHEME.length + 1 + PRISM_METHOD.toString().length + 1 + CANONICAL_SUFFIX_LENGTH
    require(
      string.length == expectedLength,
      s"Invalid canonical Prism DID length, expected: $expectedLength, actual: ${string.length}"
    )

    val prismDid = fromDid(Did.fromString(string))

    prismDid match {
      case canonicalPrismDid: CanonicalPrismDid => canonicalPrismDid
      case _ => throw new IllegalArgumentException(s"Canonical form Prism DID expected: $string")
    }

  }

  def buildCanonical(stateHash: Sha256Hash): CanonicalPrismDid = {
    val did = Did(PRISM_METHOD, DidMethodSpecificId(stateHash.hexEncoded))
    CanonicalPrismDid(did, stateHash)
  }

  def buildLongForm(stateHash: Sha256Hash, encodedState: Array[Byte]): LongFormPrismDid = {
    val atalaOperation = decodeState(stateHash, encodedState)
    val encodedStateBase64 = Base64Utils.encodeURL(encodedState)
    val methodSpecificId = DidMethodSpecificId.fromSections(Array(stateHash.hexEncoded, encodedStateBase64))
    val did = Did(PRISM_METHOD, methodSpecificId)

    longFormDidFromAtalaOperation(did, stateHash, atalaOperation)
  }

  def buildCanonicalFromOperation(atalaOperation: AtalaOperation): CanonicalPrismDid = {
    val stateHash = Sha256Hash.compute(atalaOperation.toByteArray)
    buildCanonical(stateHash)
  }

  def buildCanonicalFromKeys(
      didPublicKeys: List[DidPublicKey]
  ): CanonicalPrismDid = {
    val atalaOperation = createDidAtalaOperation(didPublicKeys)
    buildCanonicalFromOperation(atalaOperation)
  }

  def buildExperimentalCanonicalFromKeys(
      masterKey: SecpPublicKey,
      issuingKey: SecpPublicKey,
      revocationKey: SecpPublicKey
  ): CanonicalPrismDid = {
    val atalaOperation = createExperimentalDidAtalaOperation(
      masterKey = masterKey,
      issuingKey = issuingKey,
      revocationKey = revocationKey
    )

    buildCanonicalFromOperation(atalaOperation)
  }

  def buildCanonicalFromMasterPublicKey(masterKey: SecpPublicKey): CanonicalPrismDid = {
    val atalaOperation = createDidAtalaOperationFromMasterPublicKey(masterKey)
    buildCanonicalFromOperation(atalaOperation)
  }

  def buildLongFormFromOperation(atalaOperation: AtalaOperation): LongFormPrismDid = {
    val encodedState = atalaOperation.toByteArray
    val stateHash = Sha256Hash.compute(encodedState)
    buildLongForm(stateHash, encodedState)
  }

  def buildLongFormFromPublicKeys(didPublicKeys: List[DidPublicKey]): LongFormPrismDid = {
    val atalaOperation = createDidAtalaOperation(didPublicKeys)
    buildLongFormFromOperation(atalaOperation)
  }

  def buildLongFormFromMasterPublicKey(masterKey: SecpPublicKey): LongFormPrismDid = {
    val atalaOperation = createDidAtalaOperationFromMasterPublicKey(masterKey)
    buildLongFormFromOperation(atalaOperation)
  }

  def buildExperimentalLongFormFromKeys(
      masterKey: SecpPublicKey,
      issuingKey: SecpPublicKey,
      revocationKey: SecpPublicKey
  ): LongFormPrismDid = {
    val atalaOperation = createExperimentalDidAtalaOperation(
      masterKey = masterKey,
      issuingKey = issuingKey,
      revocationKey = revocationKey
    )
    buildLongFormFromOperation(atalaOperation)
  }
}

case class CanonicalPrismDid(override val did: Did, override val stateHash: Sha256Hash) extends PrismDid {
  override def asCanonical(): CanonicalPrismDid = this

  override def toString: String = did.toString()
}

case class LongFormPrismDid(override val did: Did, override val stateHash: Sha256Hash, initialState: AtalaOperation)
    extends PrismDid {
  override def asCanonical(): CanonicalPrismDid = {
    val newMethodSpecificId = DidMethodSpecificId(stateHash.hexEncoded)
    val newDid = did.copy(methodSpecificId = newMethodSpecificId)
    CanonicalPrismDid(newDid, stateHash)
  }

  override def toString: String = did.toString()
}
