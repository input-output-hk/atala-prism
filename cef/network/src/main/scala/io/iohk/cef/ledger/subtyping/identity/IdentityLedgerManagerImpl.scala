package io.iohk.cef.ledger.subtyping.identity

import akka.util.ByteString

class IdentityLedgerManagerImpl extends IdentityLedgerManager {

  override type Identity = String
  override type PublicKey = ByteString

  override val state = new State()
  override val storage = new Storage

  override val LedgerId: String = "identity-ledger"

  class Storage extends IdentityLedgerStorage {
    var list: List[Block] = List()

    override def pop(): Block = {
      val head = list.head
      list = list.tail
      head
    }

    override def push(block: Block): Unit = list = block :: list
  }

  class State(map: Map[Identity, Set[PublicKey]] = Map()) extends IdentityLedgerState {

    override def contains(identity: String): Boolean = map.contains(identity)

    override def get(identity: String): Option[Set[ByteString]] = map.get(identity)

    override def put(identity: String, publicKey: ByteString): IdentityLedgerState =
      new State(map + ((identity, get(identity).getOrElse(Set()) + publicKey)))

    override def remove(identity: String, publicKey: ByteString): IdentityLedgerState =
      new State(map + ((identity, get(identity).getOrElse(Set()) - publicKey)))
  }
}
