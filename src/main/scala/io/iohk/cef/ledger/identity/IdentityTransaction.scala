package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.crypto.{sign => signBytes, _}
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.identity.IdentityTransaction.isSignedWith
import io.iohk.cef.ledger.{LedgerError, Transaction}

sealed trait IdentityTransaction extends Transaction[IdentityData] {
  val identity: String
  val key: SigningPublicKey
  val signature: Signature
}

object IdentityTransaction {

  def sign(
      identity: String,
      `type`: IdentityTransactionType,
      publicKey: SigningPublicKey,
      privateKey: SigningPrivateKey): Signature = {
    import io.iohk.cef.codecs.nio.auto._
    val source = serializeForSignature(identity, `type`, publicKey)
    signBytes(source, privateKey)
  }

  def isSignedWith(
      signKey: SigningPublicKey,
      signature: Signature)(identity: String, `type`: IdentityTransactionType, publicKey: SigningPublicKey): Boolean = {
    import io.iohk.cef.codecs.nio.auto._

    val source = serializeForSignature(identity, `type`, publicKey)
    isValidSignature(source, signature, signKey)
  }

  private def serializeForSignature(
      identity: String,
      `type`: IdentityTransactionType,
      publicKey: SigningPublicKey): ByteString = {
    ByteString(identity) ++ ByteString(`type`.entryName) ++ publicKey.toByteString
  }
}

case class Claim(identity: String, key: SigningPublicKey, signature: Signature) extends IdentityTransaction {

  import IdentityTransaction._

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {
    val validSignature = isSignedWith(key, signature)(identity, IdentityTransactionType.Claim, key)

    if (!validSignature) {
      Left(UnableToVerifySignatureError)
    } else if (ledgerState.contains(identity)) {
      Left(IdentityTakenError(identity))
    } else {
      Right(ledgerState.put(identity, IdentityData.forKeys(key)))
    }
  }

  override def partitionIds: Set[String] = Set(identity)
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
case class Link(identity: String, key: SigningPublicKey, signature: Signature, linkingIdentitySignature: Signature)
    extends IdentityTransaction {

  import IdentityTransaction._

  /**
    * Apply this transaction to the given state
    *
    * @param ledgerState the state
    * @return the new state on success or an error if:
    *         - the identity hasn't ben claimed
    *         - the signature can't be validated
    */
  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {
    // TODO: Go directly to the expected key
    lazy val validSignature: Boolean = ledgerState
      .get(identity)
      .map(_.keys)
      .getOrElse(Set.empty)
      .exists { signKey =>
        isSignedWith(signKey, signature)(identity, IdentityTransactionType.Link, key)
      }

    lazy val validSignatureToLink =
      isSignedWith(key, linkingIdentitySignature)(identity, IdentityTransactionType.Link, key)

    if (!ledgerState.contains(identity)) {
      Left(IdentityNotClaimedError(identity))
    } else if (!validSignature) {
      Left(UnableToVerifySignatureError)
    } else if (!validSignatureToLink) {
      Left(UnableToVerifyLinkingIdentitySignatureError(identity, key))
    } else {
      val prev: IdentityData = ledgerState.get(identity).getOrElse(IdentityData.empty)
      val result = ledgerState.put(identity, prev addKey key)
      Right(result)
    }
  }

  override def partitionIds: Set[String] = Set(identity)
}

case class Unlink(identity: String, key: SigningPublicKey, signature: Signature) extends IdentityTransaction {

  import IdentityTransaction._

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {
    // TODO: Go directly to the expected key
    lazy val validSignature: Boolean = ledgerState
      .get(identity)
      .map(_.keys)
      .getOrElse(Set.empty)
      .exists { signKey =>
        isSignedWith(signKey, signature)(identity, IdentityTransactionType.Unlink, key)
      }

    if (!validSignature) {
      Left(UnableToVerifySignatureError)
    } else if (!ledgerState.contains(identity) || !ledgerState
        .get(identity)
        .map(_.keys)
        .getOrElse(Set())
        .contains(key)) {
      Left(PublicKeyNotAssociatedWithIdentity(identity, key))
    } else {
      val prev: IdentityData = ledgerState.get(identity).getOrElse(IdentityData.empty)
      val newData = prev removeKey key
      if (newData.isEmpty) {
        Right(ledgerState.remove(identity))
      } else {
        Right(ledgerState.put(identity, newData))
      }
    }
  }

  override def partitionIds: Set[String] = Set(identity)
}

/**
  *  A transaction to endorse a already claimed identity.
  * @param identity endorser's identity should be already claimed identity
  * @param key PublicKey used by endorser signature.
  * @param signature endorser's digital signature validating the transaction.
  * @param endorsedIdentity identity to endorse should be already claimed identity.
  */
case class Endorse(identity: Identity, key: SigningPublicKey, signature: Signature, endorsedIdentity: String)
    extends IdentityTransaction {

  def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] = {

    lazy val validateKey: Boolean = ledgerState
      .get(identity)
      .map(_.keys)
      .getOrElse(Set.empty)
      .contains(key)

    lazy val validSignature =
      isSignedWith(key, signature)(identity, IdentityTransactionType.Endorse, key)

    if (!ledgerState.contains(endorsedIdentity)) {
      Left(UnknownEndorsedIdentityError(endorsedIdentity))
    } else if (!ledgerState.contains(identity)) {
      Left(UnknownEndorserIdentityError(identity))
    } else if (!validSignature) {
      Left(UnableToVerifyEndorserSignatureError(identity, signature))
    } else if (!validateKey) {
      Left(PublicKeyNotAssociatedWithIdentity(identity, key))
    } else {
      val prev: IdentityData = ledgerState.get(endorsedIdentity).getOrElse(IdentityData.empty)
      val result = ledgerState.put(endorsedIdentity, prev endorse identity)
      Right(result)
    }
  }

  /**
    * The ids of the state partitions that need to be retrieved for this tx.
    * See [[io.iohk.cef.ledger.LedgerState]] for more detail.
    *
    * @return Set[String]
    */
  override def partitionIds: Set[String] = Set(identity, endorsedIdentity)
}
