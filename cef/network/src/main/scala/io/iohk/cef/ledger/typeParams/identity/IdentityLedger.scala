package io.iohk.cef.ledger.typeParams.identity

import akka.util.ByteString
import io.iohk.cef.ledger.typeParams.{Block, Ledger, LedgerStorage}

import scala.concurrent.duration._

import scala.concurrent.{Await, Future}

object IdentityLedger extends App {

  class State[I, K](map: Map[I, Set[K]] = Map[I, Set[K]]()) extends IdentityLedgerState[I, K] {

    override def contains(identity: I): Boolean = map.contains(identity)

    override def get(identity: I): Option[Set[K]] = map.get(identity)

    override def put(identity: I, publicKey: K): IdentityLedgerState[I, K] =
    new State(map + ((identity, get(identity).getOrElse(Set()) + publicKey)))

    override def remove(identity: I, publicKey: K): IdentityLedgerState[I, K] =
    new State(map + ((identity, get(identity).getOrElse(Set()) - publicKey)))
  }

  class Storage[I, K] extends LedgerStorage[Future, IdentityLedgerState[I, K]] {

    //Imagine persistent storage
    var stack = List[Block[IdentityLedgerState[I, K]]]()

    override def peek(): Future[Block[IdentityLedgerState[I, K]]] =
      Future.successful(stack.head)

    override def pop(): Future[Block[IdentityLedgerState[I, K]]] = {
      val result = Future.successful(stack.head)
      stack = stack.tail
      result
    }

    override def push(block: Block[IdentityLedgerState[I, K]]): Future[Unit] = {
      stack = block :: stack
      Future.successful(())
    }
  }

  val ledgerState = new State[String, ByteString]()

  val ledgerStorage = new Storage[String, ByteString]()

  val identityLedger = Ledger(ledgerStorage, ledgerState)

  val txs = List(
    Claim("carlos", ByteString("carlos")),
    Link("carlos", ByteString("vargas"))
  )

  val block = Block[IdentityLedgerState[String, ByteString]](1, txs)

  val newLedger = Await.result(identityLedger.apply(block), 1 second)
}
