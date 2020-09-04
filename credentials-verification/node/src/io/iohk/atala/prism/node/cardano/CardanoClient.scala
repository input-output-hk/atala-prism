package io.iohk.atala.prism.node.cardano

import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.node.cardano.CardanoClient.Result
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.models.TransactionId
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
      passphrase: String
  ): Result[PostTransactionError, TransactionId] = {
    cardanoWalletApiClient
      .postTransaction(walletId, payments, passphrase)
      .mapLeft(e => {
        logger.error(s"Could not post the Cardano transaction: $e")
        PostTransactionError.InvalidTransaction
      })
  }
}

object CardanoClient {
  type Result[E, A] = FutureEither[E, A]

  case class Config(dbSyncConfig: CardanoDbSyncClient.Config, cardanoWalletConfig: CardanoWalletApiClient.Config)

  def apply(config: Config)(implicit ec: ExecutionContext): CardanoClient = {
    new CardanoClient(CardanoDbSyncClient(config.dbSyncConfig), CardanoWalletApiClient(config.cardanoWalletConfig))
  }
}
