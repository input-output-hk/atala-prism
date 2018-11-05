package io.iohk.cef.main.builder

import akka.util.Timeout
import io.iohk.cef.codecs.nio._
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.consensus.raft._
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.utils.Logger

import scala.concurrent.ExecutionContext

abstract class ConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    commonTypeAliases: CommonTypeAliases[S, H, T]) {
  import commonTypeAliases._
  def consensus(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: NioEncDec[B],
      sByteStringSerializable: NioEncDec[S],
      dByteStringSerializable: NioEncDec[DiscoveryWireMessage]): Consensus[S, H, T]
}

class RaftConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]](
    ledgerConfigBuilder: LedgerConfigBuilder,
    transactionPoolBuilder: TransactionPoolBuilder[S, H, T],
    raftConsensusBuilder: RaftConsensusConfigBuilder[Block[S, H, T]],
    ledgerBuilder: LedgerBuilder[S, T],
    logger: Logger,
    commonTypeAliases: CommonTypeAliases[S, H, T])
    extends ConsensusBuilder[S, H, T](commonTypeAliases) {

  import commonTypeAliases._
  import ledgerBuilder._
  import ledgerConfigBuilder._
  import logger._
  import raftConsensusBuilder._
  import transactionPoolBuilder._

  private def machineCallback(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: NioEncDec[Block[S, H, T]],
      sNioEncDec: NioEncDec[S]): B => Unit = {
    val interface = txPoolFutureInterface
    val theLedger = ledger(ledgerConfig.id)
    block =>
      {
        theLedger(block) match {
          case Left(error) =>
            log.error(s"Could not apply block ${block} to the ledger with id ${ledgerConfig.id}. Error: $error")
          //TODO Crash the node
          case Right(()) =>
            val result = interface.removeBlockTransactions(block)
            if (result.isLeft) {
              log.error(s"Could not apply block ${block} to the ledger with id ${ledgerConfig.id}. Error: $result")
            }
        }
      }

  }

  private def raftNodeInterface(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: NioEncDec[B],
      sByteStringSerializable: NioEncDec[S],
      dByteStringSerializable: NioEncDec[DiscoveryWireMessage]) =
    raftNode[B](
      nodeIdStr,
      raftConfig.clusterMemberIds.toSeq,
      rpcFactory,
      raftConfig.electionTimeoutRange,
      raftConfig.heartbeatTimeoutRange,
      machineCallback,
      storage
    )

  private def raftConsensus(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: NioEncDec[B],
      sByteStringSerializable: NioEncDec[S],
      dByteStringSerializable: NioEncDec[DiscoveryWireMessage]) = new RaftConsensus(raftNodeInterface)

  override def consensus(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: NioEncDec[B],
      sByteStringSerializable: NioEncDec[S],
      dByteStringSerializable: NioEncDec[DiscoveryWireMessage]): Consensus[S, H, T] =
    new RaftConsensusInterface[S, H, T](ledgerConfig.id, raftConsensus)
}
