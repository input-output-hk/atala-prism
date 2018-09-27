package io.iohk.cef.main
import java.time.Clock

import akka.util.Timeout
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolActorModelInterface, TransactionPoolFutureInterface}

import scala.concurrent.ExecutionContext

trait TransactionPoolBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: HeaderGeneratorBuilder[S, H]
    with ConfigurationBuilder
    with LedgerStateStorageBuilder[S]
    with ActorSystemBuilder =>

  private def queue(clock: Clock): TimedQueue[T] = new TimedQueue[T](clock)

  def txPoolActorModelInterface(implicit byteStringSerializable: ByteStringSerializable[Block[S, H, T]])
    : TransactionPoolActorModelInterface[S, H, T] =
    new TransactionPoolActorModelInterface[S, H, T](
      props => actorSystem.actorOf(props),
      headerGenerator,
      maxBlockSizeInBytes,
      ledgerStateStorage,
      defaultTransactionExpiration,
      () => queue(clock)
    )

  def txPoolFutureInterface(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]]): TransactionPoolFutureInterface[S, H, T] =
    new TransactionPoolFutureInterface[S, H, T](txPoolActorModelInterface)
}
