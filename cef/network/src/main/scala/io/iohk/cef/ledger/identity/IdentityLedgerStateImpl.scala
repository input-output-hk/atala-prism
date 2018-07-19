package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.LedgerState

case class IdentityLedgerStateImpl(map: Map[String, Set[ByteString]] = Map[String, Set[ByteString]]()) extends LedgerState[String, Set[ByteString]] {

  override def equals(that: LedgerState[String, Set[ByteString]]): Boolean = this == that

  override def contains(identity: String): Boolean = map.contains(identity)

  override def get(identity: String): Option[Set[ByteString]] = map.get(identity)

  override def put(identity: String, publicKeys: Set[ByteString]): LedgerState[String, Set[ByteString]] =
    new IdentityLedgerStateImpl(map + (identity -> publicKeys))

  override def remove(identity: String): LedgerState[String, Set[ByteString]] =
    new IdentityLedgerStateImpl(map - identity)

  override def keys: Set[String] = map.keySet

}
