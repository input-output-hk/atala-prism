package io.iohk.cef.ledger.identity

import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.crypto.{sign => signBytes, _}
import io.iohk.cef.ledger.{LedgerError, Transaction}

sealed trait IdentityTransaction extends Transaction[IdentityData] {
  val data: IdentityTransactionData
  val signature: Signature
}

object IdentityTransaction {

  def sign(data: IdentityTransactionData, privateKey: SigningPrivateKey): Signature = {
    signBytes(data, privateKey)
  }

  def isDataSignedWith[D <: IdentityTransactionData: NioEncDec](
      data: D,
      publicKey: SigningPublicKey,
      signature: Signature): Boolean =
    isValidSignature(data, signature, publicKey)

  def isDataSignedWithIdentity[D <: IdentityTransactionData: NioEncDec](
      data: D,
      identity: Identity,
      state: IdentityLedgerState,
      signature: Signature): Boolean = {
    // TODO: Go directly to the expected key
    state
      .get(identity)
      .map(_.keys)
      .getOrElse(Set.empty)
      .exists(isDataSignedWith(data, _, signature))
  }
}

case class Claim(data: ClaimData, signature: Signature) extends IdentityTransaction {

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {

    if (!isValidSignature(data, signature, data.key)) {
      Left(UnableToVerifySignatureError)
    } else if (ledgerState.contains(data.identity)) {
      Left(IdentityTakenError(data.identity))
    } else {
      Right(ledgerState.put(data.identity, IdentityData.forKeys(data.key)))
    }
  }

  override def partitionIds: Set[String] = Set(data.identity)
}

/**
  * A transaction to link a new key with an existing identity.
  *
  * @param identity the identity where the new key is linked
  * @param key the public key to link to the given identity
  * @param signature a digital signature validating the transaction, it should be generated
  *                  from one of the existing keys on the given identity.
  * @param linkingIdentitySignature a digital signature validating the transaction, it should be generated
  *   *                  from the public key on the given identity.
  */
case class Link(data: LinkData, signature: Signature, linkingIdentitySignature: Signature) extends IdentityTransaction {

  /**
    * Apply this transaction to the given state
    *
    * @param ledgerState the state
    * @return the new state on success or an error if:
    *         - the identity hasn't ben claimed
    *         - the signature can't be validated
    */
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {
    lazy val providedPublicKeySignatureValid =
      isValidSignature(data, linkingIdentitySignature, data.key)
    lazy val identitySignatureValid =
      IdentityTransaction.isDataSignedWithIdentity(data, data.identity, ledgerState, signature)

    if (!ledgerState.contains(data.identity)) {
      Left(IdentityNotClaimedError(data.identity))
    } else if (!identitySignatureValid) {
      Left(UnableToVerifySignatureError)
    } else if (!providedPublicKeySignatureValid) {
      Left(UnableToVerifyLinkingIdentitySignatureError(data.identity, data.key))
    } else {
      val prev: IdentityData = ledgerState.get(data.identity).getOrElse(IdentityData.empty)
      val result = ledgerState.put(data.identity, prev addKey data.key)
      Right(result)
    }
  }

  override def partitionIds: Set[String] = Set(data.identity)
}

case class Unlink(data: UnlinkData, signature: Signature) extends IdentityTransaction {

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {
    if (!IdentityTransaction.isDataSignedWithIdentity(data, data.identity, ledgerState, signature)) {
      Left(UnableToVerifySignatureError)
    } else if (!ledgerState.contains(data.identity) || !ledgerState
        .get(data.identity)
        .map(_.keys)
        .getOrElse(Set())
        .contains(data.key)) {
      Left(PublicKeyNotAssociatedWithIdentity(data.identity, data.key))
    } else {
      val prev: IdentityData = ledgerState.get(data.identity).getOrElse(IdentityData.empty)
      val newData = prev removeKey data.key
      if (newData.isEmpty) {
        Right(ledgerState.remove(data.identity))
      } else {
        Right(ledgerState.put(data.identity, newData))
      }
    }
  }

  override def partitionIds: Set[String] = Set(data.identity)
}

/**
  *  A transaction to endorse a already claimed identity.
  * @param identity endorser's identity should be already claimed identity
  * @param key PublicKey used by endorser signature.
  * @param signature endorser's digital signature validating the transaction.
  * @param endorsedIdentity identity to endorse should be already claimed identity.
  */
case class Endorse(data: EndorseData, signature: Signature) extends IdentityTransaction {

  def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {

    if (!ledgerState.contains(data.endorsedIdentity)) {
      Left(UnknownEndorsedIdentityError(data.endorsedIdentity))
    } else if (!ledgerState.contains(data.endorserIdentity)) {
      Left(UnknownEndorserIdentityError(data.endorserIdentity))
    } else if (!IdentityTransaction.isDataSignedWithIdentity(data, data.endorserIdentity, ledgerState, signature)) {
      Left(UnableToVerifyEndorserSignatureError(data.endorserIdentity, signature))
    } else {
      val prev: IdentityData = ledgerState.get(data.endorsedIdentity).getOrElse(IdentityData.empty)
      val result = ledgerState.put(data.endorsedIdentity, prev endorse data.endorserIdentity)
      Right(result)
    }
  }

  /**
    * The ids of the state partitions that need to be retrieved for this tx.
    * See [[io.iohk.cef.ledger.LedgerState]] for more detail.
    *
    * @return Set[String]
    */
  override def partitionIds: Set[String] = Set(data.endorserIdentity, data.endorsedIdentity)
}

/**
  *  A transaction to revoke  an already endorsed identity.
  * @param identity endorser's identity should be already claimed identity
  * @param key PublicKey used by endorser signature.
  * @param signature endorser's digital signature validating the transaction.
  * @param endorsedIdentity identity to revoke should be already endorsed identity.
  */
case class RevokeEndorsement(data: RevokeEndorsementData, signature: Signature)
    extends IdentityTransaction {

  def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {

    lazy val validEndorsement: Boolean = ledgerState
      .get(data.endorsedIdentity)
      .map(_.endorsers)
      .getOrElse(Set.empty)
      .contains(data.endorserIdentity)

    if (!ledgerState.contains(data.endorserIdentity)) {
      Left(UnknownEndorserIdentityError(data.endorserIdentity))
    } else if (!ledgerState.contains(data.endorsedIdentity)) {
      Left(UnknownEndorsedIdentityError(data.endorsedIdentity))
    } else if (!IdentityTransaction.isDataSignedWithIdentity(data, data.endorserIdentity, ledgerState, signature)) {
      Left(UnableToVerifyEndorserSignatureError(data.endorserIdentity, signature))
    } else if (!validEndorsement) {
      Left(EndorsementNotAssociatedWithIdentityError(data.endorserIdentity, data.endorsedIdentity))
    } else {
      val prev: IdentityData = ledgerState.get(data.endorsedIdentity).getOrElse(IdentityData.empty)
      val result = ledgerState.put(data.endorsedIdentity, prev revoke data.endorserIdentity)
      Right(result)
    }
  }

  /**
    * The ids of the state partitions that need to be retrieved for this tx.
    * See [[io.iohk.cef.ledger.LedgerState]] for more detail.
    *
    * @return Set[String]
    */
  override def partitionIds: Set[String] = Set(data.endorserIdentity, data.endorsedIdentity)
}
