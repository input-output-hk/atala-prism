package io.iohk.cef.config

import java.time.Clock

import io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.node.OnDiskPersistentStorage
import io.iohk.cef.ledger._
import io.iohk.cef.ledger.query.{LedgerQuery, LedgerQueryService}
import io.iohk.cef.ledger.storage.{Ledger, LedgerStateStorage, LedgerStorage}
import io.iohk.cef.transactionpool.{BlockCreator, TransactionPoolInterface}
import io.iohk.cef.transactionservice._
import io.iohk.cef.transactionservice.raft.{RaftConsensusInterface, RaftRPCFactory}
import io.iohk.codecs.nio._
import io.iohk.codecs.nio.auto._
import io.iohk.network.{Envelope, Network}
import monix.reactive.MulticastStrategy
import monix.reactive.subjects.ConcurrentSubject
import org.slf4j.Logger

import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe.TypeTag

private[config] class TransactionServiceBuilder(cefConfig: CefConfig, logger: Logger, clock: Clock) {

  def cefTransactionServiceChannel[State, Tx <: Transaction[State], Q <: LedgerQuery[State]](
      ledgerStateStorage: LedgerStateStorage[State],
      ledgerStorage: LedgerStorage[State, Tx],
      queryService: LedgerQueryService[State, Q],
      appliedBlocksSubject: AppliedBlocksSubject[State, Tx]
  )(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext
  ): NodeTransactionService[State, Tx, Q] = {

    import monix.execution.Scheduler.Implicits.global

    val proposedBlocksSubject = ConcurrentSubject[Block[State, Tx]](MulticastStrategy.publish)
    val proposedTransactionsSubject = ConcurrentSubject[Tx](MulticastStrategy.publish)
    new NodeTransactionServiceImpl[State, Tx, Q](
      createLedgerServicesMap[State, Tx, Q](
        ledgerStateStorage,
        ledgerStorage,
        queryService,
        appliedBlocksSubject = appliedBlocksSubject,
        proposedBlocksSubject = proposedBlocksSubject,
        proposedTransactionsSubject = proposedTransactionsSubject
      ),
      txNetwork[State, Tx],
      blockNetwork[State, Tx],
      cefConfig.networkConfig.peerConfig.nodeId
    )
  }

  private def createLedgerServicesMap[State, Tx <: Transaction[State], Q <: LedgerQuery[State]](
      ledgerStateStorage: LedgerStateStorage[State],
      ledgerStorage: LedgerStorage[State, Tx],
      queryService: LedgerQueryService[State, Q],
      appliedBlocksSubject: AppliedBlocksSubject[State, Tx],
      proposedBlocksSubject: ProposedBlocksSubject[State, Tx],
      proposedTransactionsSubject: ProposedTransactionsSubject[Tx]
  )(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext
  ): LedgerServicesMap[State, Tx, Q] = {

    val raftConfig = cefConfig.consensusConfig.raftConfig.get
    val ledgerConfig = cefConfig.ledgerConfig

    val headerGenerator: Seq[Transaction[State]] => BlockHeader = _ => BlockHeader(clock.instant())
    val ledger: Ledger[State, Tx] = Ledger(ledgerConfig.id, ledgerStorage, ledgerStateStorage)

    val txPool = TransactionPoolInterface.apply[State, Tx](
      headerGenerator,
      ledgerConfig.maxBlockSize,
      ledgerStateStorage,
      ledgerConfig.defaultTransactionExpiration,
      proposedTransactionsSubject,
      appliedBlocksSubject
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

    val _ = (
      new RaftConsensusInterface[State, Tx](
        cefConfig.ledgerConfig.id,
        raftConsensus,
        proposedBlocksObservable = proposedBlocksSubject,
        appliedBlocksObserver = appliedBlocksSubject
      ),
      new BlockCreator(
        txPool,
        proposedBlocksSubject,
        cefConfig.ledgerConfig.blockCreatorInitialDelay,
        cefConfig.ledgerConfig.blockCreatorInterval
      )
    )

    val services = new LedgerServices(
      proposedTransactionsSubject = proposedTransactionsSubject,
      proposedBlocksSubject = proposedBlocksSubject,
      appliedBlocksSubject = appliedBlocksSubject,
      queryService
    )

    Map(cefConfig.ledgerConfig.id -> services)
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
    }
  }

  private def txNetwork[State, Tx <: Transaction[State]](
      implicit
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx]
  ): Network[Envelope[Tx]] = {
    Network[Envelope[Tx]](cefConfig.networkConfig.discovery, cefConfig.networkConfig.transports)
  }

  private def blockNetwork[State, Tx <: Transaction[State]](
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx]
  ): Network[Envelope[Block[State, Tx]]] = {

    Network[Envelope[Block[State, Tx]]](cefConfig.networkConfig.discovery, cefConfig.networkConfig.transports)
  }
}
