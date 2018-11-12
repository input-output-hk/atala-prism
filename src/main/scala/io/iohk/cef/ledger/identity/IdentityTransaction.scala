package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.crypto.{sign => signBytes, _}
import io.iohk.cef.frontend.models.IdentityTransactionType
import io.iohk.cef.ledger.{LedgerError, Transaction}
import io.iohk.cef.codecs.nio._

sealed trait IdentityTransaction extends Transaction[Set[SigningPublicKey]] {
  val identity: String
  val key: SigningPublicKey
  val signature: Signature
}

object IdentityTransaction {

  implicit val IdentityTransactionNioEncDec: NioEncDec[IdentityTransaction] = {
    import io.iohk.cef.codecs.nio.auto._
    genericEncDec
  }

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
      Right(ledgerState.put(identity, Set(key)))
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
  */
case class Link(identity: String, key: SigningPublicKey, signature: Signature) extends IdentityTransaction {

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
      .getOrElse(Set.empty)
      .exists { signKey =>
        isSignedWith(signKey, signature)(identity, IdentityTransactionType.Link, key)
      }

    if (!ledgerState.contains(identity)) {
      Left(IdentityNotClaimedError(identity))
    } else if (!validSignature) {
      Left(UnableToVerifySignatureError)
    } else {
      val result = ledgerState.put(identity, ledgerState.get(identity).getOrElse(Set()) + key)
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
      .getOrElse(Set.empty)
      .exists { signKey =>
        isSignedWith(signKey, signature)(identity, IdentityTransactionType.Unlink, key)
      }

    if (!validSignature) {
      Left(UnableToVerifySignatureError)
    } else if (!ledgerState.contains(identity) || !ledgerState.get(identity).getOrElse(Set()).contains(key)) {
      Left(PublicKeyNotAssociatedWithIdentity(identity, key))
    } else {
      val newKeys = ledgerState.get(identity).getOrElse(Set()) - key
      if (newKeys.isEmpty) {
        Right(ledgerState.remove(identity))
      } else {
        Right(ledgerState.put(identity, newKeys))
      }
    }
  }

  override def partitionIds: Set[String] = Set(identity)
}
