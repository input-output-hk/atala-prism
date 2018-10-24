package io.iohk.cef.main.builder
import akka.util.Timeout
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolInterface}

import scala.concurrent.ExecutionContext

trait TransactionPoolBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: HeaderGeneratorBuilder[S, H]
    with LedgerStateStorageBuilder[S]
    with ActorSystemBuilder
    with LedgerConfigBuilder =>

  private def queue = new TimedQueue[T](clock)

//  def txPoolActorModelInterface(
//      implicit byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
//      sByteStringSerializable: ByteStringSerializable[S]): TransactionPoolFutureInterface[S, H, T] =
//    new TransactionPoolFutureInterface[S, H, T](
//      headerGenerator,
//      ledgerConfig.maxBlockSizeInBytes,
//      ledgerStateStorage,
//      ledgerConfig.defaultTransactionExpiration,
//      () => queue
//    )

  def txPoolFutureInterface(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S]): TransactionPoolInterface[S, H, T] =
    new TransactionPoolInterface[S, H, T](
      headerGenerator,
      ledgerConfig.maxBlockSizeInBytes,
      ledgerStateStorage,
      ledgerConfig.defaultTransactionExpiration,
      () => queue)
}
