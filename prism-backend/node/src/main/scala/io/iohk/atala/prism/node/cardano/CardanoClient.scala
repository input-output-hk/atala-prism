package io.iohk.atala.prism.node.cardano

import cats.effect.{IO, Resource}
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.node.cardano.CardanoClient.Result
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class CardanoClient(cardanoDbSyncClient: CardanoDbSyncClient, cardanoWalletApiClient: CardanoWalletApiClient)(implicit
    ec: ExecutionContext
) {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def getFullBlock(blockNo: Int): Result[BlockError.NotFound, Block.Full] = {
    cardanoDbSyncClient.getFullBlock(blockNo)
  }

  def getLatestBlock(): Result[BlockError.NoneAvailable.type, Block.Canonical] = {
    cardanoDbSyncClient.getLatestBlock()
  }

  def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): Result[CardanoWalletError, TransactionId] = {
    cardanoWalletApiClient
      .postTransaction(walletId, payments, metadata, passphrase)
      .mapLeft(e => {
        logger.error(s"Could not post the Cardano transaction: ${e.error}")
        CardanoWalletError.fromString(e.error.message, e.error.code)
      })
  }

  def getTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Result[CardanoWalletError, TransactionDetails] = {
    cardanoWalletApiClient
      .getTransaction(walletId, transactionId)
      .mapLeft(e => {
        logger.error(s"Could not get Cardano transaction $transactionId: ${e.error}")
        CardanoWalletError.fromString(e.error.message, e.error.code)
      })
  }

  def deleteTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Result[CardanoWalletError, Unit] = {
    cardanoWalletApiClient
      .deleteTransaction(walletId, transactionId)
      .mapLeft(e => {
        logger.error(s"Could not delete Cardano transaction $transactionId: ${e.error}")
        CardanoWalletError.fromString(e.error.message, e.error.code)
      })
  }
}

object CardanoClient {
  type Result[E, A] = FutureEither[E, A]

  case class Config(dbSyncConfig: CardanoDbSyncClient.Config, cardanoWalletConfig: CardanoWalletApiClient.Config)

  def apply(config: Config)(implicit ec: ExecutionContext): Resource[IO, CardanoClient] = {
    CardanoDbSyncClient(config.dbSyncConfig).map(cardanoDbSyncClient =>
      new CardanoClient(cardanoDbSyncClient, CardanoWalletApiClient(config.cardanoWalletConfig))
    )
  }
}
