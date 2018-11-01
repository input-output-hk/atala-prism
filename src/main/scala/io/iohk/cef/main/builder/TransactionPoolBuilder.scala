package io.iohk.cef.main.builder
import akka.util.Timeout
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.transactionpool.{TimedQueue, TransactionPoolInterface}

import scala.concurrent.ExecutionContext

class TransactionPoolBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    headerGeneratorBuilder: LedgerHeaderGenerator[S, H],
    ledgerStateStorageBuilder: LedgerStateStorageBuilder[S],
    ledgerConfigBuilder: LedgerConfigBuilder
) {
  import headerGeneratorBuilder._
  import ledgerConfigBuilder._
  import ledgerStateStorageBuilder._

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
