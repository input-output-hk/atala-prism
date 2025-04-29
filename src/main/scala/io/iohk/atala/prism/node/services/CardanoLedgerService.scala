package io.iohk.atala.prism.node.services

import cats.Applicative
import cats.Comonad
import cats.Functor
import cats.Monad
import cats.effect.Resource
import cats.effect.Temporal
import cats.syntax.all._
import enumeratum.Enum
import enumeratum.EnumEntry
import io.iohk.atala.prism.node.PublicationInfo
import io.iohk.atala.prism.node.UnderlyingLedger
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_NO
import io.iohk.atala.prism.node.cardano.LAST_SYNCED_BLOCK_TIMESTAMP
import io.iohk.atala.prism.node.cardano.models.Block.Canonical
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.models.Balance
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoBlockHandler
import io.iohk.atala.prism.node.services.CardanoLedgerService.CardanoNetwork
import io.iohk.atala.prism.node.services.logs.UnderlyingLedgerLogs
import io.iohk.atala.prism.node.services.models.AtalaObjectBulkNotificationHandler
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.services.models.AtalaObjectNotificationHandler
import io.iohk.atala.prism.protos.node_models
import tofu.higherKind.Mid
import tofu.lift.Lift
import tofu.logging.Logs
import tofu.logging.ServiceLogging

import scala.concurrent.Future
import scala.concurrent.duration._

/** Implements ledger structure based on Cardano. Interacts with the cardano wallet using REST API requests. This
  * service also tracks all Cardano blocks and retrieves relevant PRISM operations from transaction metadata.
  *
  * @param network
  *   defines Cardano deployment (TESTNET, MAINNET).
  * @param walletId
  *   identifier of the Cardano wallet we use for publishing new transactions.
  * @param walletPassphrase
  *   secret passphrase for the wallet.
  * @param paymentAddress
  *   every transaction should have a payment address.
  * @param blockNumberSyncStart
  *   service looks for PRISM operations in Cardano blocks with number greater than `blockNumberSyncStart`.
  * @param blockConfirmationsToWait
  *   minimum block confirmations required to consider operations "confirmed".
  * @param cardanoClient
  *   service for creating REST API calls to the Cardano Wallet.
  * @param keyValueService
  *   service for storing useful internal data such as the number of the latest synchronized cardano block.
  * @param onCardanoBlock
  *   callback on every new confirmed Cardano block.
  * @param onAtalaObject
  *   callback on every AtalaObject found in confirmed transactions.
  */
class CardanoLedgerService[F[_]] private[services] (
    network: CardanoNetwork,
    walletId: WalletId,
    walletPassphrase: String,
    paymentAddress: Address,
    blockNumberSyncStart: Int,
    blockConfirmationsToWait: Int,
    cardanoClient: CardanoClient[F],
    keyValueService: KeyValueService[F],
    onCardanoBlock: CardanoBlockHandler[F],
    onAtalaObject: AtalaObjectNotificationHandler[F],
    onAtalaObjectBulk: AtalaObjectBulkNotificationHandler[F]
)(implicit
    timer: Temporal[F],
    liftToFuture: Lift[F, Future]
) extends UnderlyingLedger[F] {
  private val MAX_SYNC_BLOCKS = 100

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

  /** Publishes AtalaObject containing a list of operations inside.
    */
  override def publish(
      obj: node_models.AtalaObject
  ): F[Either[CardanoWalletError, PublicationInfo]] = {
    val metadata = AtalaObjectMetadata.toTransactionMetadata(obj)

    for {
      // send new creation request to the Cardano wallet
      txId <- cardanoClient
        .postTransaction(
          walletId,
          List(Payment(paymentAddress, minUtxoDeposit)),
          Some(metadata),
          walletPassphrase
        )
    } yield txId.map(transactionId =>
      PublicationInfo(
        TransactionInfo(transactionId, getType),
        TransactionStatus.Pending
      )
    )
  }

  /** Retrieves the current transaction status by its identifier
    */
  override def getTransactionDetails(
      transactionId: TransactionId
  ): F[Either[CardanoWalletError, TransactionDetails]] =
    cardanoClient.getTransaction(walletId, transactionId)

  /** Deletes the transaction from the queue in the Cardano Wallet if it's possible. Returns
    * `CardanoWalletError.TransactionAlreadyInLedger` if the transaction was already published on the Ledger.
    */
  override def deleteTransaction(
      transactionId: TransactionId
  ): F[Either[CardanoWalletError, Unit]] =
    cardanoClient.deleteTransaction(walletId, transactionId)

  override def getWalletBalance: F[Either[CardanoWalletError, Balance]] =
    cardanoClient.getWalletDetails(walletId).map(_.map(_.balance))

  /** After the initial `delay`, schedules synchronization of PRISM Node with Cardano ledger.
    */
  private def scheduleSync(delay: FiniteDuration): Unit = {
    liftToFuture.lift(
      timer.sleep(delay) >>
        syncAtalaObjects()
          .recover { case _ =>
            false
          }
          .map { pendingBlocksToSync =>
            if (pendingBlocksToSync) {
              // There blocks to sync, don't wait to sync faster
              scheduleSync(0.seconds)
            } else {
              scheduleSync(20.seconds)
            }
          }
    )
    ()
  }

  /** Sync Atala objects from blocks and returns whether there are remaining blocks to sync.
    */
  private[services] def syncAtalaObjects(): F[Boolean] = {
    for {
      // Gets the number of the latest block processed by PRISM Node.
      maybeLastSyncedBlockNo <- keyValueService.getInt(LAST_SYNCED_BLOCK_NO)
      // Calculates the next block based on the initial `blockNumberSyncStart` and the latest synced block.
      lastSyncedBlockNo = CardanoLedgerService.calculateLastSyncedBlockNo(
        maybeLastSyncedBlockNo,
        blockNumberSyncStart
      )

      _ <-
        if (lastSyncedBlockNo == (blockNumberSyncStart - 1)) {
          bulkSyncBlocks()
        } else {
          Monad[F].pure(())
        }
      maybeLastSyncedBlockNo <- keyValueService.getInt(LAST_SYNCED_BLOCK_NO)
      // Calculates the next block based on the initial `blockNumberSyncStart` and the latest synced block.
      lastSyncedBlockNo = CardanoLedgerService.calculateLastSyncedBlockNo(
        maybeLastSyncedBlockNo,
        blockNumberSyncStart
      )
      // Gets the latest block from the Cardano database.
      latestBlockOpt <- cardanoClient.getLatestBlock
      // Calculates the latest confirmed block based on amount of required confirmations.
      lastConfirmedBlockNo = latestBlockOpt.map(
        _.header.blockNo - blockConfirmationsToWait
      )
      syncStart = lastSyncedBlockNo + 1
      // Sync no more than `MAX_SYNC_BLOCKS` during one `syncAtalaObjects` iteration.
      syncEnd = lastConfirmedBlockNo.map(
        math.min(_, lastSyncedBlockNo + MAX_SYNC_BLOCKS)
      )
      // Sync all blocks with numbers from `syncStart` to `syncEnd`
      _ <- syncEnd.traverse(end => syncBlocks(syncStart to end))
    } yield lastConfirmedBlockNo
      .flatMap(last => syncEnd.map(last > _))
      .getOrElse(false)
  }

  private def bulkSyncBlocks(): F[Unit] = {
    for {
      blocksEit <- cardanoClient.getAllPrismIndexBlocksWithTransactions()
      _ <- blocksEit.fold(
        _ => Monad[F].pure(()),
        blocks =>
          for {
            // Bulk process all Atala objects
            _ <- bulkProcessAtalaObjects(blocks)
            // Gets the latest block from the Cardano database.
            latestBlockOpt <- cardanoClient.getLatestBlock
            // Calculates the latest confirmed block based on amount of required confirmations.
            lastConfirmedBlockNo = latestBlockOpt.map(
              _.header.blockNo - blockConfirmationsToWait
            )

            _ <- lastConfirmedBlockNo match {
              case Right(value) =>
                cardanoClient.getFullBlock(value).flatMap {
                  case Right(confirmedBlock) => {
                    updateLastSyncedBlock(confirmedBlock)
                  }
                  case Left(_) => {
                    updateLastSyncedBlock(blocks.last)
                  }
                }
              case Left(_) => {
                updateLastSyncedBlock(blocks.last)
              }
            }
          } yield ()
      )
    } yield ()
  }

  private def bulkProcessAtalaObjects(blocks: List[Block.Full]): F[Unit] = {
    // Collect all notifications from blocks
    val notifications: List[AtalaObjectNotification] = blocks.flatMap { block =>
      block.transactions.flatMap { transaction =>
        transaction.metadata.flatMap { metadata =>
          AtalaObjectMetadata.fromTransactionMetadata(metadata).map { atalaObject =>
            AtalaObjectNotification(
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
          }
        }
      }
    }

    val batchSize = 5000 // TODO: make it configurable

    // notifications.grouped(batchSize).toList.traverse_(onAtalaObjectBulk)
    notifications.grouped(batchSize).toList.traverse_ { batch =>
      println(
        s"Processing ${batch.size} notifications in batches of size: ${batch.size}"
      )
      onAtalaObjectBulk(batch)
    }
  }

  // Sync blocks in the given range.
  private def syncBlocks(blockNos: Range): F[Unit] = {
    if (blockNos.isEmpty) {
      ().pure[F]
    } else {
      // Sequentially sync blocks from the given range one by one.
      blockNos.foldLeft(().pure[F]) { case (previous, blockNo) =>
        for {
          _ <- previous
          _ <- syncBlock(blockNo)
        } yield ()
      }
    }
  }

  /** Sync block `blockNo` with internal state.
    */
  private def syncBlock(blockNo: Int): F[Unit] = {
    for {
      // Retrieve block header and the list of transactions in the block.
      blockEit <- cardanoClient
        .getFullBlock(blockNo)
      // Trigger the callback `onCardanoBlock`.
      _ <-
        blockEit
          .map(block => onCardanoBlock(block.toCanonical))
          .getOrElse(Monad[F].pure(new Exception("syncBlock failed")))
      // Look over transactions in the block.
      _ <- blockEit.traverse(processAtalaObjects)
    } yield ()
  }

  /** Sequentially sync transactions in the `block`.
    */
  private def processAtalaObjects(block: Block.Full): F[Unit] = {
    val notifications: List[AtalaObjectNotification] = for {
      // Iterate over transactions in the block.
      transaction <- block.transactions
      // Retrieve metadata from the transaction if it exists.
      metadata <- transaction.metadata
      // Parse metadata in accordance with the PRISM protocol if it's possible.
      atalaObject <- AtalaObjectMetadata.fromTransactionMetadata(metadata)
      // Create new notification if `atalaObject` was successfully retrieved from the transaction metadata.
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

    for {
      // Trigger `onAtalaObject` callback on every successfully parsed AtalaObject.
      _ <- notifications.traverse(onAtalaObject)
      // Update the number of the latest synchronized block.
      _ <- updateLastSyncedBlock(block)
    } yield ()
  }

  private def updateLastSyncedBlock(block: Block.Full): F[Unit] = {
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
  }
}

object CardanoLedgerService {

  type CardanoBlockHandler[F[_]] = Canonical => F[Unit]

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

  def apply[F[_]: Lift[*[_], Future]: Temporal, R[_]: Functor](
      network: CardanoNetwork,
      walletId: WalletId,
      walletPassphrase: String,
      paymentAddress: Address,
      blockNumberSyncStart: Int,
      blockConfirmationsToWait: Int,
      cardanoClient: CardanoClient[F],
      keyValueService: KeyValueService[F],
      onCardanoBlock: CardanoBlockHandler[F],
      onAtalaObject: AtalaObjectNotificationHandler[F],
      onAtalaObjectBulk: AtalaObjectBulkNotificationHandler[F],
      logs: Logs[R, F]
  ): R[UnderlyingLedger[F]] =
    for {
      serviceLogs <- logs.service[UnderlyingLedger[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, UnderlyingLedger[F]] =
        serviceLogs
      val logs: UnderlyingLedger[Mid[F, *]] = new UnderlyingLedgerLogs[F]
      val mid = logs
      mid attach new CardanoLedgerService[F](
        network,
        walletId,
        walletPassphrase,
        paymentAddress,
        blockNumberSyncStart,
        blockConfirmationsToWait,
        cardanoClient,
        keyValueService,
        onCardanoBlock,
        onAtalaObject,
        onAtalaObjectBulk
      )
    }

  def resource[F[_]: Temporal: Lift[*[_], Future], R[_]: Applicative](
      config: Config,
      cardanoClient: CardanoClient[F],
      keyValueService: KeyValueService[F],
      onCardanoBlock: CardanoBlockHandler[F],
      onAtalaObject: AtalaObjectNotificationHandler[F],
      onAtalaObjectBulk: AtalaObjectBulkNotificationHandler[F],
      logs: Logs[R, F]
  ): Resource[R, UnderlyingLedger[F]] = {
    val walletId = WalletId
      .from(config.walletId)
      .getOrElse(
        throw new IllegalArgumentException(
          s"Wallet ID ${config.walletId} is invalid"
        )
      )
    val walletPassphrase = config.walletPassphrase
    val paymentAddress = Address(config.paymentAddress)

    Resource.eval(
      CardanoLedgerService(
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
        onAtalaObjectBulk,
        logs
      )
    )
  }

  def unsafe[F[_]: Lift[*[_], Future]: Temporal, R[_]: Comonad](
      config: Config,
      cardanoClient: CardanoClient[F],
      keyValueService: KeyValueService[F],
      onCardanoBlock: CardanoBlockHandler[F],
      onAtalaObject: AtalaObjectNotificationHandler[F],
      onAtalaObjectBulk: AtalaObjectBulkNotificationHandler[F],
      logs: Logs[R, F]
  ): UnderlyingLedger[F] = {
    val walletId = WalletId
      .from(config.walletId)
      .getOrElse(
        throw new IllegalArgumentException(
          s"Wallet ID ${config.walletId} is invalid"
        )
      )
    val walletPassphrase = config.walletPassphrase
    val paymentAddress = Address(config.paymentAddress)

    CardanoLedgerService(
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
      onAtalaObjectBulk,
      logs
    ).extract
  }

  def calculateLastSyncedBlockNo(
      maybeLastSyncedBlockNo: Option[Int],
      blockNumberSyncStart: Int
  ): Int =
    math.max(maybeLastSyncedBlockNo.getOrElse(0), blockNumberSyncStart - 1)

}
