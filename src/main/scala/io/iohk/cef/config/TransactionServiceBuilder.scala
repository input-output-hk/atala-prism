package io.iohk.cef.config

import java.time.Clock

import io.iohk.codecs.nio._
import io.iohk.codecs.nio.auto._
import io.iohk.cef.consensus.raft.node.OnDiskPersistentStorage
import io.iohk.cef.consensus.{Consensus, raft}
import io.iohk.cef.ledger.storage.{Ledger, LedgerStateStorage, LedgerStorage}
import io.iohk.cef.ledger.{Block, BlockHeader, LedgerId, Transaction}
import io.iohk.network.{Envelope, Network}
import io.iohk.cef.transactionpool.{BlockCreator, TransactionPoolInterface}
import io.iohk.cef.transactionservice.raft.{RaftConsensusInterface, RaftRPCFactory}
import io.iohk.cef.transactionservice.NodeTransactionService
import org.slf4j.Logger

import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._

private[config] class TransactionServiceBuilder(
    cefConfig: CefConfig,
    logger: Logger,
    clock: Clock
) {

  def cefTransactionServiceChannel[State, Tx <: Transaction[State]](
      ledgerStateStorage: LedgerStateStorage[State],
      ledgerStorage: LedgerStorage[State, Tx]
  )(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext
  ): NodeTransactionService[State, Tx] = {

    new NodeTransactionService[State, Tx](
      consensusMap[State, Tx](ledgerStateStorage, ledgerStorage),
      txNetwork[State, Tx],
      blockNetwork[State, Tx],
      cefConfig.networkConfig.peerConfig.nodeId
    )
  }

  private def consensusMap[State, Tx <: Transaction[State]](
      ledgerStateStorage: LedgerStateStorage[State],
      ledgerStorage: LedgerStorage[State, Tx]
  )(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext
  ): Map[LedgerId, (TransactionPoolInterface[State, Tx], Consensus[State, Tx])] = {

    val raftConfig = cefConfig.consensusConfig.raftConfig.get
    val ledgerConfig = cefConfig.ledgerConfig

    val headerGenerator: Seq[Transaction[State]] => BlockHeader = _ => BlockHeader(clock.instant())
    val ledger: Ledger[State, Tx] = Ledger(ledgerConfig.id, ledgerStorage, ledgerStateStorage)

    val txPool = TransactionPoolInterface[State, Tx](
      headerGenerator,
      ledgerConfig.maxBlockSize,
      ledgerStateStorage,
      ledgerConfig.defaultTransactionExpiration
    )

    val raftNode = raft.raftNode(
      raftConfig.nodeId,
      raftConfig.clusterMemberIds,
      new RaftRPCFactory[Block[State, Tx]](cefConfig.networkConfig.discovery, cefConfig.networkConfig.transports),
      raftConfig.electionTimeoutRange,
      raftConfig.heartbeatTimeoutRange,
      stateMachineCallback(ledger, txPool),
      new OnDiskPersistentStorage[Block[State, Tx]](raftConfig.nodeId)
    )

    val raftConsensus: raft.RaftConsensus[Block[State, Tx]] = new raft.RaftConsensus(raftNode)

    val consensus = new RaftConsensusInterface[State, Tx](cefConfig.ledgerConfig.id, raftConsensus)

    val _ = new BlockCreator(
      txPool,
      consensus,
      cefConfig.ledgerConfig.blockCreatorInitialDelay,
      cefConfig.ledgerConfig.blockCreatorInterval
    )

    Map(cefConfig.ledgerConfig.id -> (txPool, consensus))
  }

  private def stateMachineCallback[State, Tx <: Transaction[State]](
      ledger: Ledger[State, Tx],
      txPool: TransactionPoolInterface[State, Tx]
  )(block: Block[State, Tx])(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx]
  ): Unit = {
    ledger(block) match {
      case Left(error) =>
        logger.error(s"Could not apply block $block to the ledger with id ${ledger.ledgerId}. Error: $error")
      case Right(()) =>
        val result = txPool.removeBlockTransactions(block)
        if (result.isLeft) {
          logger.error(s"Could not apply block $block to the ledger with id ${ledger.ledgerId}. Error: $result")
        }
    }
  }

  private def txNetwork[State, Tx <: Transaction[State]](
      implicit
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx]
  ): Network[Envelope[Tx]] =
    Network[Envelope[Tx]](cefConfig.networkConfig.discovery, cefConfig.networkConfig.transports)

  private def blockNetwork[State, Tx <: Transaction[State]](
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx]
  ): Network[Envelope[Block[State, Tx]]] = {

    Network[Envelope[Block[State, Tx]]](cefConfig.networkConfig.discovery, cefConfig.networkConfig.transports)
  }
}
