package io.iohk.cef.integration
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestActorRef
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.ledger.storage.LedgerStateStorage
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolActorModelInterface}
import io.iohk.cef.utils.ByteSizeable

import scala.concurrent.duration.Duration

trait TxPoolFixture {

  class TestableTransactionPoolActorModelInterface[State, Header <: BlockHeader, Tx <: Transaction[State]]( headerGenerator: Seq[Transaction[State]] => Header,
                                                                                                            maxTxSizeInBytes: Int,
                                                                                                            ledgerStateStorage: LedgerStateStorage[State],
                                                                                                            defaultDurationTxs: Duration,
                                                                                                            timedQueue: TimedQueue[Tx])(implicit blockByteSizeable: ByteSizeable[Block[State, Header, Tx]], system: ActorSystem)
    extends TransactionPoolActorModelInterface[State, Header, Tx](
      system.actorOf,
      headerGenerator,
      maxTxSizeInBytes,
      ledgerStateStorage,
      defaultDurationTxs,
      () => timedQueue) {

    lazy val testActorRef = TestActorRef[TransactionPoolActor](Props(new TransactionPoolActor()))
    override def poolActor: ActorRef = testActorRef
  }
}
