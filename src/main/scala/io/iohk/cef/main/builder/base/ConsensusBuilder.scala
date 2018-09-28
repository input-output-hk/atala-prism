package io.iohk.cef.main.builder.base
import akka.util.Timeout
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.consensus.raft._
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.main.builder.derived.TransactionPoolBuilder
import scala.concurrent.ExecutionContext

trait ConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  def consensus(
                 implicit timeout: Timeout,
                 executionContext: ExecutionContext,
                 byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
                 sByteStringSerializable: ByteStringSerializable[S]): Consensus[S, H, T]
}

trait RaftConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] extends ConsensusBuilder[S, H, T] {
  self: LedgerConfigBuilder
    with TransactionPoolBuilder[S, H, T]
    with RaftConsensusConfigBuilder[Block[S, H, T]] =>
  type B = Block[S, H, T]

  private def machineCallback(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S]): B => Unit = {
      val interface = txPoolFutureInterface
      block => {
        interface.removeBlockTxs(block)
        //TODO: Log errors
      }
    }

  private def raftNodeInterface(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S]) =
    raftNode[B](
      nodeId,
      clusterMemberIds,
      rpcFactory,
      electionTimeoutRange,
      heartbeatTimeoutRange,
      machineCallback,
      storage
      )

  private def raftConsensus(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S]) = new RaftConsensus(raftNodeInterface)

  override def consensus(
                          implicit timeout: Timeout,
                          executionContext: ExecutionContext,
                          byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
                          sByteStringSerializable: ByteStringSerializable[S]): Consensus[S, H, T] =
    new RaftConsensusInterface[S, H, T](ledgerId, raftConsensus)
}
