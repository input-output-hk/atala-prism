package io.iohk.node.cardano

import io.iohk.cvp.utils.FutureEither
import io.iohk.node.cardano.CardanoClient.Result
import io.iohk.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.node.cardano.models._
import io.iohk.node.cardano.wallet.CardanoWalletApiClient

import scala.concurrent.ExecutionContext

class CardanoClient(cardanoDbSyncClient: CardanoDbSyncClient, cardanoWalletApiClient: CardanoWalletApiClient)(implicit
    ec: ExecutionContext
) {
  def getBlock(hash: BlockHash): Result[BlockError.NotFound, Block.Canonical] = {
    cardanoDbSyncClient.getBlock(hash)
  }

  def getFullBlock(hash: BlockHash): Result[BlockError.NotFound, Block.Full] = {
    cardanoDbSyncClient.getFullBlock(hash)
  }

  def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      passphrase: String
  ): Result[PostTransactionError, TransactionId] = {
    cardanoWalletApiClient
      .postTransaction(walletId, payments, passphrase)
      .mapLeft(_ => PostTransactionError.InvalidTransaction)
  }
}

object CardanoClient {
  type Result[E, A] = FutureEither[E, A]

  case class Config(dbSyncConfig: CardanoDbSyncClient.Config, cardanoWalletConfig: CardanoWalletApiClient.Config)

  def apply(config: Config)(implicit ec: ExecutionContext): CardanoClient = {
    new CardanoClient(CardanoDbSyncClient(config.dbSyncConfig), CardanoWalletApiClient(config.cardanoWalletConfig))
  }
}
