package io.iohk.cef.ledger.identity

import java.security.{PrivateKey, PublicKey, SecureRandom}

import akka.util.ByteString
import io.iohk.cef.crypto.low.{DigitalSignature, SignAlgorithm}
import io.iohk.cef.ledger.{LedgerError, Transaction}

sealed trait IdentityTransaction extends Transaction[Set[PublicKey]] {
  val identity: String
  val key: PublicKey
  override def hashCode(): Int = (identity.hashCode) + (key.hashCode())
}

case class Claim(identity: String, key: PublicKey) extends IdentityTransaction {

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(ledgerState.contains(identity)) {
      Left(IdentityTakenError(identity))
    } else {
      Right(ledgerState.put(identity, Set(key)))
    }

  override def partitionIds: Set[String] = Set(identity)

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case Claim(i, k) => i == identity && k == key
      case _ => false
    }
  }

  override def canEqual(that: Any): Boolean = that.isInstanceOf[Claim]
}

/**
 * A transaction to link a new key with an existing identity.
 *
 * @param identity the identity where the new key is linked
 * @param key the public key to link to the given identity
 * @param signature a digital signature validating the transaction, it should be generated
 *                  from one of the existing keys on the given identity.
 */
case class Link(identity: String, key: PublicKey, signature: DigitalSignature) extends IdentityTransaction {

  import Link._

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
          isSignedWith(signKey, signature)(identity, key)
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

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case Link(i, k) => i == identity && k == key
      case _ => false
    }
  }

  override def canEqual(that: Any): Boolean = that.isInstanceOf[Link]
}

case class Unlink(identity: String, key: PublicKey) extends IdentityTransaction {

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(!ledgerState.contains(identity) || !ledgerState.get(identity).getOrElse(Set()).contains(key)) {
      Left(PublicKeyNotAssociatedWithIdentity(identity, key))
    } else {
      if(ledgerState.get(identity).get.size == 1) {
        Right(ledgerState.remove(identity))
      } else {
        Right(ledgerState.put(identity, ledgerState.get(identity).getOrElse(Set()) - key))
      }
    }

  override def partitionIds: Set[String] = Set(identity)

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case Unlink(i, k) => i == identity && k == key
      case _ => false
    }
  }

  override def canEqual(that: Any): Boolean = that.isInstanceOf[Unlink]
}
