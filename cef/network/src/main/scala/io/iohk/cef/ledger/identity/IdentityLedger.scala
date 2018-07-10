package io.iohk.cef.ledger.identity

import akka.util.ByteString
import io.iohk.cef.ledger.storage.{Ledger, LedgerStorage}
import io.iohk.cef.ledger.{Block, BlockHeader}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object IdentityLedger extends App {

  class LedgerStateImpl(map: Map[String, Set[ByteString]] = Map[String, Set[ByteString]]()) extends IdentityLedgerState {

    override def hash: ByteString = ByteString(map.hashCode())

    override def contains(identity: String): Boolean = map.contains(identity)

    override def get(identity: String): Option[Set[ByteString]] = map.get(identity)

    override def put(identity: String, publicKey: ByteString): IdentityLedgerState =
    new LedgerStateImpl(map + ((identity, get(identity).getOrElse(Set()) + publicKey)))

    override def remove(identity: String, publicKey: ByteString): IdentityLedgerState =
    new LedgerStateImpl(map + ((identity, get(identity).getOrElse(Set()) - publicKey)))

    override def keys: Set[String] = map.keySet

    override def iterator: Iterator[(String, Set[ByteString])] = map.iterator
  }

  class Storage extends LedgerStorage[Future, IdentityLedgerState, String] {

    //Imagine persistent storage
    var stack = List[Block[IdentityLedgerState, String]]()

    override def push(block: Block[IdentityLedgerState, String]): Future[Unit] = {
      stack = block :: stack
      Future.successful(Right(()))
    }
  }

  val ledgerStateStorage = new StateStorage()

  val ledgerStorage = new Storage()

  val identityLedger = Ledger(ledgerStorage, ledgerStateStorage)

  val txs = List(
    Claim("carlos", ByteString("carlos")),
    Link("carlos", ByteString("vargas"))
  )

  val block = Block(new BlockHeader {}, txs)

  val newLedger = identityLedger.apply(block).map(future => Await.result(future, 1 second))
}
