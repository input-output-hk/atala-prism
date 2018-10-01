package io.iohk.cef.main.builder.base
import akka.util.Timeout
import io.iohk.cef.consensus.Consensus
import io.iohk.cef.consensus.raft._
import io.iohk.cef.ledger.{Block, BlockHeader, ByteStringSerializable, Transaction}
import io.iohk.cef.main.builder.derived.{LedgerBuilder, LogBuilder, TransactionPoolBuilder}
import io.iohk.cef.network.discovery.DiscoveryWireMessage
import io.iohk.cef.utils.ForExpressionsEnabler

import scala.concurrent.{ExecutionContext, Future}

trait ConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] {
  def consensus(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      lByteStringSerializable: ByteStringSerializable[LogEntry[Block[S, H, T]]]): Consensus[S, H, T]
}

trait RaftConsensusBuilder[S, H <: BlockHeader, T <: Transaction[S]] extends ConsensusBuilder[S, H, T] {
  self: LedgerConfigBuilder
    with TransactionPoolBuilder[S, H, T]
    with RaftConsensusConfigBuilder[Block[S, H, T]]
    with LedgerConfigBuilder
    with LedgerBuilder[Future, S, T]
    with LogBuilder
    with CommonTypeAliases[S, H, T] =>

  private def machineCallback(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S]): B => Unit = {
    implicit val enabler = ForExpressionsEnabler.futureEnabler
    val interface = txPoolFutureInterface
    val theLedger = ledger(ledgerId)
    block =>
      {
        theLedger(block) match {
          case Left(error) =>
            log.error(s"Could not apply block ${block} to the ledger with id ${ledgerId}. Error: $error")
          //TODO Crash the node
          case Right(ledgerDatabaseResultFuture) =>
            val result = for {
              _ <- ledgerDatabaseResultFuture
              txPoolResult <- interface.removeBlockTxs(block)
            } yield txPoolResult
            result onComplete {
              case scala.util.Success(value) if value.isLeft =>
                log.error(s"Could not apply block ${block} to the ledger with id ${ledgerId}. Error: $value")
              //TODO Crash the node
              case scala.util.Failure(exception) =>
                log.error(s"Could not apply block ${block} to the ledger with id ${ledgerId}. Error: $exception")
              //TODO Crash the node
              case scala.util.Success(_) =>
                log.info(s"Successfully applied block ${block} to the ledger with id ${ledgerId}")
            }
        }
      }
  }

  private def raftNodeInterface(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      lByteStringSerializable: ByteStringSerializable[LogEntry[Block[S, H, T]]]) =
    raftNode[B](
      nodeIdStr,
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
      sByteStringSerializable: ByteStringSerializable[S],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      lByteStringSerializable: ByteStringSerializable[LogEntry[Block[S, H, T]]]) = new RaftConsensus(raftNodeInterface)

  override def consensus(
      implicit timeout: Timeout,
      executionContext: ExecutionContext,
      byteStringSerializable: ByteStringSerializable[Block[S, H, T]],
      sByteStringSerializable: ByteStringSerializable[S],
      dByteStringSerializable: ByteStringSerializable[DiscoveryWireMessage],
      lByteStringSerializable: ByteStringSerializable[LogEntry[Block[S, H, T]]]): Consensus[S, H, T] =
    new RaftConsensusInterface[S, H, T](ledgerId, raftConsensus)
}
