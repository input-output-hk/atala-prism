package io.iohk.atala.prism.node.services

import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.models.{Ledger, TransactionInfo}
import io.iohk.atala.prism.node.AtalaReferenceLedger
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.models.{TransactionMetadata, _}
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoNetwork
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.prism.protos.node_internal
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class CardanoLedgerService private[services] (
    network: CardanoNetwork,
    walletId: WalletId,
    walletPassphrase: String,
    paymentAddress: Address,
    blockConfirmationsToWait: Int,
    cardanoClient: CardanoClient,
    keyValueService: KeyValueService,
    onAtalaObject: AtalaObjectNotificationHandler,
    scheduler: Scheduler
)(implicit
    ec: ExecutionContext
) extends AtalaReferenceLedger {
  private val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"

  private val logger = LoggerFactory.getLogger(this.getClass)

  // Minimum amount that can be deposited in Cardano, from
  // https://github.com/input-output-hk/cardano-node/blob/1f0171d96443eaf7a77072397e790b514b670414/configuration/cardano/shelley-genesis.json#L18
  private val minUtxoDeposit = Lovelace(1000000)

  private val ledger: Ledger = {
    if (network == CardanoNetwork.Testnet) {
      Ledger.CardanoTestnet
    } else {
      Ledger.CardanoMainnet
    }
  }

  // Schedule the initial sync
  scheduleSync(30.seconds)

  override def supportsOnChainData: Boolean = false

  override def publish(obj: node_internal.AtalaObject): Future[TransactionInfo] = {
    val metadata = TransactionMetadata.fromProto(obj)
    cardanoClient
      .postTransaction(walletId, List(Payment(paymentAddress, minUtxoDeposit)), Some(metadata), walletPassphrase)
      .value
      .map {
        case Right(transactionId) => TransactionInfo(transactionId, ledger)
        case Left(error) =>
          logger.error(s"FATAL: Error while publishing reference: $error")
          throw new RuntimeException(s"FATAL: Error while publishing reference: $error")
      }
  }

  private def scheduleSync(delay: FiniteDuration): Unit = {
    scheduler.scheduleOnce(delay) {
      // Ensure run is scheduled after completion, even if current run fails
      syncAtalaObjects()
        .recover {
          case e => logger.error(s"Could not sync Atala objects", e)
        }
        .onComplete { _ =>
          scheduleSync(20.seconds)
        }
    }
    ()
  }

  private[services] def syncAtalaObjects(): Future[Unit] = {
    for {
      lastSyncedBlockNo <- keyValueService.getInt(LAST_SYNCED_BLOCK_NO)
      latestBlock <- cardanoClient.getLatestBlock().toFuture(_ => new RuntimeException("Cardano blockchain is empty"))
      syncStart = lastSyncedBlockNo.getOrElse(0) + 1
      syncEnd = latestBlock.header.blockNo - blockConfirmationsToWait
      _ <- syncBlocks(syncStart to syncEnd)
    } yield ()
  }

  private def syncBlocks(blockNos: Range): Future[Unit] = {
    if (blockNos.isEmpty) {
      Future.unit
    } else {
      blockNos.foldLeft(Future.unit) {
        case (previous, blockNo) =>
          for {
            _ <- previous
            _ <- syncBlock(blockNo)
          } yield ()
      }
    }
  }

  private def syncBlock(blockNo: Int): Future[Unit] = {
    for {
      block <- cardanoClient.getFullBlock(blockNo).toFuture(_ => new RuntimeException(s"Block $blockNo was not found"))
      _ <- processAtalaObjects(block)
    } yield ()
  }

  private def processAtalaObjects(block: Block.Full): Future[Unit] = {
    val notifications: List[AtalaObjectNotification] = block.transactions.flatMap { _ =>
      // TODO: Extract Atala reference when metadata is available
      None
    }
    if (notifications.nonEmpty) {
      logger trace s"Found ${notifications.size} ATALA references in block ${block.header.blockNo}"
    }

    for {
      _ <- Future.traverse(notifications) { onAtalaObject(_) }
      _ <- keyValueService.set(LAST_SYNCED_BLOCK_NO, Some(block.header.blockNo))
    } yield ()
  }
}

object CardanoLedgerService {

  type Result[E, A] = FutureEither[E, A]

  sealed trait CardanoNetwork extends EnumEntry
  object CardanoNetwork extends Enum[CardanoNetwork] {
    val values = findValues

    case object Testnet extends CardanoNetwork
    case object Mainnet extends CardanoNetwork
  }

  case class Config(
      network: CardanoNetwork,
      walletId: String,
      walletPassphrase: String,
      paymentAddress: String,
      blockConfirmationsToWait: Int,
      cardanoClientConfig: CardanoClient.Config
  )

  def apply(config: Config, keyValueService: KeyValueService, onAtalaObject: AtalaObjectNotificationHandler)(implicit
      scheduler: Scheduler
  ): CardanoLedgerService = {
    val walletId = WalletId
      .from(config.walletId)
      .getOrElse(throw new IllegalArgumentException(s"Wallet ID ${config.walletId} is invalid"))
    val walletPassphrase = config.walletPassphrase
    val paymentAddress = Address(config.paymentAddress)
    val cardanoClient = CardanoClient(config.cardanoClientConfig)

    new CardanoLedgerService(
      config.network,
      walletId,
      walletPassphrase,
      paymentAddress,
      config.blockConfirmationsToWait,
      cardanoClient,
      keyValueService,
      onAtalaObject,
      scheduler
    )
  }
}
