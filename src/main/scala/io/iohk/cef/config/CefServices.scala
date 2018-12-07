package io.iohk.cef.config
import java.nio.file.Files
import java.time.Clock

import io.iohk.cef.LedgerId
import io.iohk.cef.codecs.nio._
import io.iohk.cef.codecs.nio.auto._
import io.iohk.cef.consensus.raft.node.OnDiskPersistentStorage
import io.iohk.cef.consensus.{Consensus, raft}
import io.iohk.cef.ledger.storage.mv.{MVLedgerStateStorage, MVLedgerStorage}
import io.iohk.cef.transactionservice.raft.{RaftConsensusInterface, RaftRPCFactory}
import io.iohk.cef.transactionservice.{Envelope, NodeTransactionService}
import io.iohk.cef.ledger.storage.{Ledger, LedgerStorage}
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}
import io.iohk.cef.network.transport.Transports
import io.iohk.cef.network.{Network, NetworkServices}
import io.iohk.cef.transactionpool.TransactionPoolInterface
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._

object CefServices {
  def cefTransactionServiceChannel[State, Tx <: Transaction[State]](cefConfig: CefConfig)(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext): NodeTransactionService[State, Tx] = {

    new CefServices(cefConfig).cefTransactionServiceChannel()
  }
}

private[config] class CefServices(cefConfig: CefConfig) {

  // TODO consider adding logger name and timezone to config
  private val log = LoggerFactory.getLogger("cef")
  private val clock = Clock.systemUTC()

  private val transports = new Transports(cefConfig.peerConfig)

  private val networkDiscovery =
    NetworkServices.networkDiscovery(clock, cefConfig.peerConfig, cefConfig.discoveryConfig)

  def cefTransactionServiceChannel[State, Tx <: Transaction[State]]()(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext): NodeTransactionService[State, Tx] = {

    new NodeTransactionService[State, Tx](
      consensusMap[State, Tx],
      txNetwork[State, Tx],
      blockNetwork[State, Tx],
      cefConfig.peerConfig.nodeId
    )
  }

  private def consensusMap[State, Tx <: Transaction[State]]()(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx],
      ec: ExecutionContext): Map[LedgerId, (TransactionPoolInterface[State, Tx], Consensus[State, Tx])] = {

    val raftConfig = cefConfig.consensusConfig.raftConfig.get
    val ledgerConfig = cefConfig.ledgerConfig

    val headerGenerator: Seq[Transaction[State]] => BlockHeader = _ => BlockHeader(clock.instant())

    val ledgerStateStoragePath = Files.createTempFile(s"state-storage-${ledgerConfig.id}", "").toAbsolutePath
    val ledgerStateStorage = new MVLedgerStateStorage[State](ledgerConfig.id, ledgerStateStoragePath)

    val ledgerStoragePath = Files.createTempFile(s"ledger-storage-${ledgerConfig.id}", "").toAbsolutePath
    val ledgerStorage: LedgerStorage[State, Tx] = new MVLedgerStorage[State, Tx](ledgerConfig.id, ledgerStoragePath)

    val ledger: Ledger[State, Tx] = Ledger(ledgerConfig.id, ledgerStorage, ledgerStateStorage)

    val txPool = TransactionPoolInterface[State, Tx](
      headerGenerator,
      ledgerConfig.maxBlockSize,
      ledgerStateStorage,
      ledgerConfig.defaultTransactionExpiration)

    val raftNode = raft.raftNode(
      raftConfig.nodeId,
      raftConfig.clusterMemberIds,
      new RaftRPCFactory[Block[State, Tx]](networkDiscovery, transports),
      raftConfig.electionTimeoutRange,
      raftConfig.heartbeatTimeoutRange,
      stateMachineCallback(ledger, txPool),
      new OnDiskPersistentStorage[Block[State, Tx]](raftConfig.nodeId)
    )

    val raftConsensus: raft.RaftConsensus[Block[State, Tx]] = new raft.RaftConsensus(raftNode)

    val consensus = new RaftConsensusInterface[State, Tx](cefConfig.ledgerConfig.id, raftConsensus)

    Map(cefConfig.ledgerConfig.id -> (txPool, consensus))
  }

  private def stateMachineCallback[State, Tx <: Transaction[State]](
      ledger: Ledger[State, Tx],
      txPool: TransactionPoolInterface[State, Tx])(block: Block[State, Tx])(
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx]): Unit = {
    ledger(block) match {
      case Left(error) =>
        log.error(s"Could not apply block $block to the ledger with id ${ledger.ledgerId}. Error: $error")
      case Right(()) =>
        val result = txPool.removeBlockTransactions(block)
        if (result.isLeft) {
          log.error(s"Could not apply block $block to the ledger with id ${ledger.ledgerId}. Error: $result")
        }
    }
  }

  private def txNetwork[State, Tx <: Transaction[State]](
      implicit
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx]): Network[Envelope[Tx]] =
    Network[Envelope[Tx]](networkDiscovery, transports)

  private def blockNetwork[State, Tx <: Transaction[State]](
      implicit stateCodec: NioCodec[State],
      stateTypeTag: TypeTag[State],
      txCodec: NioCodec[Tx],
      txTypeTag: TypeTag[Tx]): Network[Envelope[Block[State, Tx]]] =
    Network[Envelope[Block[State, Tx]]](networkDiscovery, transports)
}
