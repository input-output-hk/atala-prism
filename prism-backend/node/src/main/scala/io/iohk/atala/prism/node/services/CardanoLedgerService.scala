package io.iohk.atala.prism.node.services

import cats.{Comonad, Functor}
import cats.effect.MonadThrow
import cats.syntax.applicativeError._
import cats.syntax.applicative._
import cats.syntax.comonad._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import enumeratum.{Enum, EnumEntry}
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
import io.iohk.atala.prism.node.repositories.daos.KeyValuesDAO.KeyValue
import io.iohk.atala.prism.node.services.CardanoLedgerService.{CardanoBlockHandler, CardanoNetwork}
import io.iohk.atala.prism.node.services.logs.UnderlyingLedgerLogs
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.node.{PublicationInfo, UnderlyingLedger}
import io.iohk.atala.prism.protos.node_internal
import monix.execution.Scheduler
import org.slf4j.LoggerFactory
import tofu.Execute
import tofu.higherKind.Mid
import tofu.lift.Lift
import tofu.logging.{Logs, ServiceLogging}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class CardanoLedgerService[F[_]: MonadThrow] private[services] (
    network: CardanoNetwork,
    walletId: WalletId,
    walletPassphrase: String,
    paymentAddress: Address,
    blockNumberSyncStart: Int,
    blockConfirmationsToWait: Int,
    cardanoClient: CardanoClient[F],
    keyValueService: KeyValueService[F],
    onCardanoBlock: CardanoBlockHandler,
    onAtalaObject: AtalaObjectNotificationHandler,
    scheduler: Scheduler
)(implicit
    ec: ExecutionContext,
    ex: Execute[F],
    liftToFuture: Lift[F, Future]
) extends UnderlyingLedger[F] {
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

  override def publish(obj: node_internal.AtalaObject): F[Either[CardanoWalletError, PublicationInfo]] = {
    val metadata = AtalaObjectMetadata.toTransactionMetadata(obj)

    for {
      txId <- cardanoClient
        .postTransaction(walletId, List(Payment(paymentAddress, minUtxoDeposit)), Some(metadata), walletPassphrase)
    } yield txId.map(transactionId =>
      PublicationInfo(TransactionInfo(transactionId, getType), TransactionStatus.Pending)
    )
  }

  override def getTransactionDetails(
      transactionId: TransactionId
  ): F[Either[CardanoWalletError, TransactionDetails]] = cardanoClient.getTransaction(walletId, transactionId)

  override def deleteTransaction(transactionId: TransactionId): F[Either[CardanoWalletError, Unit]] =
    cardanoClient.deleteTransaction(walletId, transactionId)

  private def scheduleSync(delay: FiniteDuration): Unit = {
    scheduler.scheduleOnce(delay) {
      // Ensure run is scheduled after completion, even if current run fails
      liftToFuture
        .lift(
          syncAtalaObjects()
            .recover { case e =>
              logger.error("Could not sync Atala objects", e)
              false
            }
        )
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
  private[services] def syncAtalaObjects(): F[Boolean] = {
    for {
      maybeLastSyncedBlockNo <- keyValueService.getInt(LAST_SYNCED_BLOCK_NO)
      lastSyncedBlockNo = CardanoLedgerService.calculateLastSyncedBlockNo(maybeLastSyncedBlockNo, blockNumberSyncStart)
      latestBlock <- cardanoClient.getLatestBlock
      lastConfirmedBlockNo = latestBlock.map(_.header.blockNo - blockConfirmationsToWait)
      syncStart = lastSyncedBlockNo + 1
      syncEnd = lastConfirmedBlockNo.map(math.min(_, lastSyncedBlockNo + MAX_SYNC_BLOCKS))
      _ <- syncEnd.traverse(end => syncBlocks(syncStart to end))
    } yield lastConfirmedBlockNo.flatMap(last => syncEnd.map(last > _)).getOrElse(false)
  }

  private def syncBlocks(blockNos: Range): F[Unit] = {
    if (blockNos.isEmpty) {
      ().pure[F]
    } else {
      blockNos.foldLeft(().pure[F]) { case (previous, blockNo) =>
        for {
          _ <- previous
          _ <- syncBlock(blockNo)
        } yield ()
      }
    }
  }

  private def syncBlock(blockNo: Int): F[Unit] = {
    for {
      blockEit <- cardanoClient
        .getFullBlock(blockNo)
      _ <- ex.deferFuture(
        blockEit
          .map(block => onCardanoBlock(block.toCanonical))
          .getOrElse(Future.failed(new Exception("syncBlock failed")))
      )
      _ <- blockEit.traverse(processAtalaObjects)
    } yield ()
  }

  private def processAtalaObjects(block: Block.Full): F[Unit] = {
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
      _ <- ex.deferFuture(notifications.traverse(onAtalaObject))
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

  type CardanoBlockHandler = Canonical => Future[Unit]

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

  def apply[F[_]: MonadThrow: Execute: Lift[*[_], Future], R[_]: Functor](
      network: CardanoNetwork,
      walletId: WalletId,
      walletPassphrase: String,
      paymentAddress: Address,
      blockNumberSyncStart: Int,
      blockConfirmationsToWait: Int,
      cardanoClient: CardanoClient[F],
      keyValueService: KeyValueService[F],
      onCardanoBlock: CardanoBlockHandler,
      onAtalaObject: AtalaObjectNotificationHandler,
      scheduler: Scheduler,
      logs: Logs[R, F]
  )(implicit ec: ExecutionContext): R[UnderlyingLedger[F]] =
    for {
      serviceLogs <- logs.service[UnderlyingLedger[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, UnderlyingLedger[F]] = serviceLogs
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
        scheduler
      )
    }

  def unsafe[F[_]: MonadThrow: Execute, R[_]: Comonad](
      config: Config,
      cardanoClient: CardanoClient[F],
      keyValueService: KeyValueService[F],
      onCardanoBlock: CardanoBlockHandler,
      onAtalaObject: AtalaObjectNotificationHandler,
      logs: Logs[R, F]
  )(implicit
      scheduler: Scheduler,
      liftToFuture: Lift[F, Future]
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
      scheduler,
      logs
    ).extract
  }

  def calculateLastSyncedBlockNo(
      maybeLastSyncedBlockNo: Option[Int],
      blockNumberSyncStart: Int
  ): Int =
    math.max(maybeLastSyncedBlockNo.getOrElse(0), blockNumberSyncStart - 1)

}
