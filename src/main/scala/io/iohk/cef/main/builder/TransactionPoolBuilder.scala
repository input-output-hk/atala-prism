package io.iohk.cef.main.builder
import akka.util.Timeout
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.transactionpool.TransactionPoolInterface

import scala.concurrent.ExecutionContext
import io.iohk.cef.codecs.nio._

class TransactionPoolBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    headerGeneratorBuilder: LedgerHeaderGenerator[S, H],
    ledgerStateStorageBuilder: LedgerStateStorageBuilder[S],
    ledgerConfigBuilder: LedgerConfigBuilder
) {
  import headerGeneratorBuilder._
  import ledgerConfigBuilder._
  import ledgerStateStorageBuilder._

  def txPoolFutureInterface(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: NioEncDec[Block[S, H, T]],
      sNioEncDec: NioEncDec[S]): TransactionPoolInterface[S, H, T] =
    TransactionPoolInterface[S, H, T](
      headerGenerator,
      ledgerConfig.maxBlockSizeInBytes,
      ledgerStateStorage,
      ledgerConfig.defaultTransactionExpiration,
      clock)
}
