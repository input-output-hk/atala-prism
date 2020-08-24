package io.iohk.atala.prism.node.services

import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.utils.FutureEither
import io.iohk.atala.prism.node.AtalaReferenceLedger
import io.iohk.atala.prism.node.cardano.CardanoClient
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.services.models.{AtalaObjectUpdate, ObjectHandler}
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class CardanoLedgerService private[services] (
    walletId: WalletId,
    walletPassphrase: String,
    paymentAddress: Address,
    blockConfirmationsToWait: Int,
    cardanoClient: CardanoClient,
    keyValueService: KeyValueService,
    onNewObject: ObjectHandler,
    scheduler: Scheduler
)(implicit
    ec: ExecutionContext
) extends AtalaReferenceLedger {
  private val LAST_SYNCED_BLOCK_NO = "last_synced_block_no"

  private val logger = LoggerFactory.getLogger(this.getClass)

  // Minimum amount that can be deposited in Cardano, from
  // https://github.com/input-output-hk/cardano-node/blob/1f0171d96443eaf7a77072397e790b514b670414/configuration/cardano/shelley-genesis.json#L18
  private val minUtxoDeposit = Lovelace(1000000)

  // Schedule the initial sync
  scheduleSync(30.seconds)

  override def supportsOnChainData: Boolean = false

  override def publishReference(ref: SHA256Digest): Future[Unit] = {
    // TODO: Send `ref` as metadata
    cardanoClient
      .postTransaction(walletId, List(Payment(paymentAddress, minUtxoDeposit)), walletPassphrase)
      .map(_ => ())
      .toFuture(_ => new RuntimeException("Could not publish reference"))
  }

  override def publishObject(bytes: Array[Byte]): Future[Unit] = {
    throw new NotImplementedError("Publishing whole objects not implemented for Cardano ledger")
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
    val atalaReferences: List[SHA256Digest] = block.transactions.flatMap { _ =>
      // TODO: Extract Atala reference when metadata is available
      None
    }
    if (atalaReferences.nonEmpty) {
      logger trace s"Found ${atalaReferences.size} ATALA references in block ${block.header.blockNo}"
    }

    for {
      _ <- Future.traverse(atalaReferences) { reference =>
        onNewObject(AtalaObjectUpdate.Reference(reference), block.header.time)
      }
      _ <- keyValueService.set(LAST_SYNCED_BLOCK_NO, Some(block.header.blockNo))
    } yield ()
  }
}

object CardanoLedgerService {

  type Result[E, A] = FutureEither[E, A]

  case class Config(
      walletId: String,
      walletPassphrase: String,
      paymentAddress: String,
      blockConfirmationsToWait: Int,
      cardanoClientConfig: CardanoClient.Config
  )

  def apply(config: Config, keyValueService: KeyValueService, onNewObject: ObjectHandler)(implicit
      scheduler: Scheduler
  ): CardanoLedgerService = {
    val walletId = WalletId
      .from(config.walletId)
      .getOrElse(throw new IllegalArgumentException(s"Wallet ID ${config.walletId} is invalid"))
    val walletPassphrase = config.walletPassphrase
    val paymentAddress = Address(config.paymentAddress)
    val cardanoClient = CardanoClient(config.cardanoClientConfig)

    new CardanoLedgerService(
      walletId,
      walletPassphrase,
      paymentAddress,
      config.blockConfirmationsToWait,
      cardanoClient,
      keyValueService,
      onNewObject,
      scheduler
    )
  }
}
