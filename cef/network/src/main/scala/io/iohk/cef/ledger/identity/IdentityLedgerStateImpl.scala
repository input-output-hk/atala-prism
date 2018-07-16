package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.LedgerState

case class IdentityLedgerStateImpl(map: Map[String, Set[ByteString]] = Map[String, Set[ByteString]]()) extends IdentityLedgerState {

  override def equals(that: LedgerState[String, Set[ByteString]]): Boolean = this == that

  override def contains(identity: String): Boolean = map.contains(identity)

  override def get(identity: String): Option[Set[ByteString]] = map.get(identity)

  override def put(identity: String, publicKey: ByteString): IdentityLedgerState =
    new IdentityLedgerStateImpl(map + ((identity, get(identity).getOrElse(Set()) + publicKey)))

  override def remove(identity: String, publicKey: ByteString): IdentityLedgerState =
    new IdentityLedgerStateImpl(map + ((identity, get(identity).getOrElse(Set()) - publicKey)))

  override def keys: Set[String] = map.keySet

}
