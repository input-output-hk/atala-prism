package io.iohk.cef.main.builder

import akka.util.Timeout
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.consensus.raft._
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.utils.Logger

import scala.concurrent.ExecutionContext

trait ConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  self: CommonTypeAliases[S, H, T] =>
  def consensus(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[B],
      sByteStringSerializable: ByteStringSerializable[S],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): Consensus[S, H, T]
}

trait RaftConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] extends ConsensusBuilder[S, H, T] {
  self: LedgerConfigBuilder
    with TransactionPoolBuilder[S, H, T]
    with RaftConsensusConfigBuilder[Block[S, H, T]]
    with LedgerConfigBuilder
    with LedgerBuilder[S, T]
    with Logger
    with CommonTypeAliases[S, H, T] =>

  private def machineCallback(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S]): B => Unit = {
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
      byteStringSerializable: ByteStringSerializable[B],
      sByteStringSerializable: ByteStringSerializable[S],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]) =
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
      byteStringSerializable: ByteStringSerializable[B],
      sByteStringSerializable: ByteStringSerializable[S],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]) = new RaftConsensus(raftNodeInterface)

  override def consensus(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[B],
      sByteStringSerializable: ByteStringSerializable[S],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage]): Consensus[S, H, T] =
    new RaftConsensusInterface[S, H, T](ledgerConfig.id, raftConsensus)
}
