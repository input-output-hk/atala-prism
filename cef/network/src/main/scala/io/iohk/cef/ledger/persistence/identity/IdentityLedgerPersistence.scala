package io.iohk.cef.ledger.persistence.identity

import akka.util.ByteString
import io.iohk.cef.ledger.persistence.{Block, BlockHeader, Ledger, LedgerStorage}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object IdentityLedgerPersistence extends App {

  class Storage extends LedgerStorage[Future, PersistentIdentityLedgerState] {

    //Imagine persistent storage
    var stack = List[Block[PersistentIdentityLedgerState]]()

    override def peek(): Future[Block[PersistentIdentityLedgerState]] =
      Future.successful(stack.head)

    override def pop(): Future[Block[PersistentIdentityLedgerState]] = {
      val result = Future.successful(stack.head)
      stack = stack.tail
      result
    }

    override def push(block: Block[PersistentIdentityLedgerState]): Future[Unit] = {
      stack = block :: stack
      Future.successful(())
    }
  }

  val ledgerState: PersistentIdentityLedgerState = new PersistentIdentityLedgerStateImpl

  val ledgerStorage: LedgerStorage[Future, PersistentIdentityLedgerState] = new Storage()

  val identityLedger = Ledger[PersistentIdentityLedgerState](ledgerStorage, ledgerState)

  val txs1 = List(
    ClaimP("carlos", ByteString("carlos"))
  )

  val txs2 = List(
    LinkP("carlos", ByteString("vargas")),
    LinkP("carlos", ByteString("montero")),
    LinkP("carlos", ByteString("roberto")),
    UnlinkP("carlos", ByteString("vargas")),
    ClaimP("carlos", ByteString("carlos"))
  )

  val block1 = Block[PersistentIdentityLedgerState](new BlockHeader {}, txs1)
  val block2 = Block[PersistentIdentityLedgerState](new BlockHeader {}, txs2)

  val newLedger1 = Await.result(identityLedger.apply(block1), 100 seconds)
  val newLedger2 = Await.result(newLedger1.apply(block2), 100 seconds)

  println()
}
