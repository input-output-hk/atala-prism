package io.iohk.cef.main.builder.derived
import akka.util.Timeout
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.main.builder.base.{
  HeaderGeneratorBuilder,
  LedgerConfigBuilder,
  LedgerStateStorageBuilder
}
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolActorModelInterface, TransactionPoolFutureInterface}

import scala.concurrent.ExecutionContext

trait TransactionPoolBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: HeaderGeneratorBuilder[S, H]
    with LedgerStateStorageBuilder[S]
    with ActorSystemBuilder
    with LedgerConfigBuilder =>

  private def queue = new TimedQueue[T](clock)

  def txPoolActorModelInterface(implicit byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
                                sByteStringSerializable: ByteStringSerializable[S])
    : TransactionPoolActorModelInterface[S, H, T] =
    new TransactionPoolActorModelInterface[S, H, T](
      props => actorSystem.actorOf(props),
      headerGenerator,
      ledgerConfig.maxBlockSizeInBytes,
      ledgerStateStorage,
      ledgerConfig.defaultTransactionExpiration,
      () => queue
    )

  def txPoolFutureInterface(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S]): TransactionPoolFutureInterface[S, H, T] =
    new TransactionPoolFutureInterface[S, H, T](txPoolActorModelInterface)
}
