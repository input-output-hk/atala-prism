package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.{LedgerError, Transaction}

sealed trait IdentityTransaction extends Transaction[Set[PublicKey]] {
  val identity: String
  val key: PublicKey
  override def hashCode(): Int = (identity.hashCode) + (key.hashCode())
}

case class Claim(identity: String, key: ByteString) extends IdentityTransaction {

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

case class Link(identity: String, key: PublicKey) extends IdentityTransaction{

  override def apply(ledgerState: IdentityLedgerState): Either[LedgerError, IdentityLedgerState] =
    if(!ledgerState.contains(identity)) {
      Left(IdentityNotClaimedError(identity))
    } else {
      Right(ledgerState.put(identity, ledgerState.get(identity).getOrElse(Set()) + key))
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
