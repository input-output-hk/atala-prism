package io.iohk.atala.prism.node.services

import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.{
  BlockInfo,
  Ledger,
  TransactionDetails,
  TransactionId,
  TransactionInfo,
  TransactionStatus
}
import io.iohk.atala.prism.node.cardano.models.Block.Canonical
import io.iohk.atala.prism.node.cardano.{CardanoClient, LAST_SYNCED_BLOCK_NO, LAST_SYNCED_BLOCK_TIMESTAMP}
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.logging.NodeLogging.logOperationIds
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import io.iohk.atala.prism.node.services.CardanoLedgerService.{CardanoBlockHandler, CardanoNetwork}
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.node.{PublicationInfo, UnderlyingLedger}
import io.iohk.atala.prism.protos.node_internal
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class CardanoLedgerService private[services] (
    network: CardanoNetwork,
    walletId: WalletId,
    walletPassphrase: String,
    paymentAddress: Address,
    blockNumberSyncStart: Int,
    blockConfirmationsToWait: Int,
    cardanoClient: CardanoClient,
    keyValueService: KeyValueService[IOWithTraceIdContext],
    onCardanoBlock: CardanoBlockHandler,
    onAtalaObject: AtalaObjectNotificationHandler,
    scheduler: Scheduler
)(implicit
    ec: ExecutionContext
) extends UnderlyingLedger {
  private val MAX_SYNC_BLOCKS = 100

  private val logger = LoggerFactory.getLogger(this.getClass)

  // Minimum amount that can be deposited in Cardano, from
  // https://github.com/input-output-hk/cardano-node/blob/1f0171d96443eaf7a77072397e790b514b670414/configuration/cardano/shelley-genesis.json#L18
  private val minUtxoDeposit = Lovelace(1000000)

  override def getType: Ledger = {
    if (network == CardanoNetwork.Testnet) {
      Ledger.CardanoTestnet
    } else {
      Ledger.CardanoMainnet
    }
  }

  // Schedule the initial sync
  scheduleSync(30.seconds)

  override def publish(
      obj: node_internal.AtalaObject
  ): Future[Either[CardanoWalletError, PublicationInfo]] = {
    val metadata = AtalaObjectMetadata.toTransactionMetadata(obj)
    cardanoClient
      .postTransaction(
        walletId,
        List(Payment(paymentAddress, minUtxoDeposit)),
        Some(metadata),
        walletPassphrase
      )
      .mapLeft { error =>
        logOperationIds(
          "publish",
          s"FATAL: Error while publishing reference: ${error.code}",
          obj
        )(logger)
        error
      }
      .map { transactionId =>
        PublicationInfo(
          TransactionInfo(transactionId, getType),
          TransactionStatus.Pending
        )
      }
      .value
  }

  override def getTransactionDetails(
      transactionId: TransactionId
  ): Future[Either[CardanoWalletError, TransactionDetails]] = {
    cardanoClient
      .getTransaction(walletId, transactionId)
      .mapLeft { error =>
        val errorMessage =
          s"FATAL: Error while getting transaction details: ${error.code}"
        logger.error(
          s"methodName: getTransactionDetails , message: $errorMessage",
          error
        )
        error
      }
      .value
  }

  override def deleteTransaction(
      transactionId: TransactionId
  ): Future[Either[CardanoWalletError, Unit]] = {
    cardanoClient
      .deleteTransaction(walletId, transactionId)
      .mapLeft { error =>
        val errorMessage = s"Could not delete transaction $transactionId"
        logger.error(
          s"methodName: deleteTransaction , message: $errorMessage",
          error
        )
        error
      }
      .value
  }

  private def scheduleSync(delay: FiniteDuration): Unit = {
    scheduler.scheduleOnce(delay) {
      // Ensure run is scheduled after completion, even if current run fails
      syncAtalaObjects()
        .recover { case e =>
          logger.error(s"Could not sync Atala objects", e)
          false
        }
        .onComplete { pendingBlocksToSync =>
          if (pendingBlocksToSync.toOption.getOrElse(false)) {
            // There blocks to sync, don't wait to sync faster
            scheduleSync(0.seconds)
          } else {
            scheduleSync(20.seconds)
          }
        }
    }
    ()
  }

  /** Syncs Atala objects from blocks and returns whether there are remaining blocks to sync.
    */
  private[services] def syncAtalaObjects(): Future[Boolean] = {
    val tId = TraceId.generateYOLO
    for {
      maybeLastSyncedBlockNo <- keyValueService
        .getInt(LAST_SYNCED_BLOCK_NO)
        .run(tId)
        .unsafeToFuture()
      lastSyncedBlockNo = CardanoLedgerService.calculateLastSyncedBlockNo(
        maybeLastSyncedBlockNo,
        blockNumberSyncStart
      )
      latestBlock <-
        cardanoClient
          .getLatestBlock(tId)
          .toFuture(_ => new RuntimeException("Cardano blockchain is empty"))
      lastConfirmedBlockNo =
        latestBlock.header.blockNo - blockConfirmationsToWait
      syncStart = lastSyncedBlockNo + 1
      syncEnd = math.min(
        lastConfirmedBlockNo,
        lastSyncedBlockNo + MAX_SYNC_BLOCKS
      )
      _ <- syncBlocks(syncStart to syncEnd)
    } yield lastConfirmedBlockNo > syncEnd
  }

  private def syncBlocks(blockNos: Range): Future[Unit] = {
    if (blockNos.isEmpty) {
      Future.unit
    } else {
      blockNos.foldLeft(Future.unit) { case (previous, blockNo) =>
        for {
          _ <- previous
          _ <- syncBlock(blockNo)
        } yield ()
      }
    }
  }

  private def syncBlock(blockNo: Int): Future[Unit] = {
    for {
      block <-
        cardanoClient
          .getFullBlock(blockNo, TraceId.generateYOLO)
          .toFuture(_ => new RuntimeException(s"Block $blockNo was not found"))
      _ <- onCardanoBlock(block.toCanonical)
      _ <- processAtalaObjects(block)
    } yield ()
  }

  private def processAtalaObjects(block: Block.Full): Future[Unit] = {
    val notifications: List[AtalaObjectNotification] = for {
      transaction <- block.transactions
      metadata <- transaction.metadata
      atalaObject <- AtalaObjectMetadata.fromTransactionMetadata(metadata)
      notification = AtalaObjectNotification(
        atalaObject,
        TransactionInfo(
          transactionId = transaction.id,
          ledger = getType,
          block = Some(
            BlockInfo(
              number = block.header.blockNo,
              timestamp = block.header.time,
              index = transaction.blockIndex
            )
          )
        )
      )
    } yield notification

    if (notifications.nonEmpty) {
      logger.info(
        s"Found ${notifications.size} Atala objects in block ${block.header.blockNo}"
      )
    }

    for {
      _ <- Future.traverse(notifications) { onAtalaObject(_) }
      _ <- updateLastSyncedBlock(block)
    } yield ()
  }

  private def updateLastSyncedBlock(block: Block.Full): Future[Unit] = {
    val timestampEpochMilli = block.header.time.toEpochMilli
    keyValueService
      .setMany(
        List(
          KeyValue(LAST_SYNCED_BLOCK_NO, Some(block.header.blockNo.toString)),
          KeyValue(
            LAST_SYNCED_BLOCK_TIMESTAMP,
            Some(timestampEpochMilli.toString)
          )
        )
      )
      .run(TraceId.generateYOLO)
      .unsafeToFuture()
  }
}

object CardanoLedgerService {

  type CardanoBlockHandler = Canonical => Future[Unit]

  type Result[E, A] = Future[Either[E, A]]

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
      blockNumberSyncStart: Int,
      blockConfirmationsToWait: Int,
      cardanoClientConfig: CardanoClient.Config
  )

  def apply(
      config: Config,
      cardanoClient: CardanoClient,
      keyValueService: KeyValueService[IOWithTraceIdContext],
      onCardanoBlock: CardanoBlockHandler,
      onAtalaObject: AtalaObjectNotificationHandler
  )(implicit
      scheduler: Scheduler
  ): CardanoLedgerService = {
    val walletId = WalletId
      .from(config.walletId)
      .getOrElse(
        throw new IllegalArgumentException(
          s"Wallet ID ${config.walletId} is invalid"
        )
      )
    val walletPassphrase = config.walletPassphrase
    val paymentAddress = Address(config.paymentAddress)

    new CardanoLedgerService(
      config.network,
      walletId,
      walletPassphrase,
      paymentAddress,
      config.blockNumberSyncStart,
      config.blockConfirmationsToWait,
      cardanoClient,
      keyValueService,
      onCardanoBlock,
      onAtalaObject,
      scheduler
    )
  }

  def calculateLastSyncedBlockNo(
      maybeLastSyncedBlockNo: Option[Int],
      blockNumberSyncStart: Int
  ): Int =
    math.max(maybeLastSyncedBlockNo.getOrElse(0), blockNumberSyncStart - 1)

}
