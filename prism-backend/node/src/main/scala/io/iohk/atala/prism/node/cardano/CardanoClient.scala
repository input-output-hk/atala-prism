package io.iohk.atala.prism.node.cardano

import cats.effect.{ContextShift, IO, Resource}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.node.cardano.CardanoClient.Result
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.models.WalletDetails
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.atala.prism.utils.IOUtils._
import org.slf4j.{Logger, LoggerFactory}
import tofu.logging.Logs

import scala.concurrent.ExecutionContext

class CardanoClient(
    cardanoDbSyncClient: CardanoDbSyncClient[IOWithTraceIdContext],
    cardanoWalletApiClient: CardanoWalletApiClient
)(implicit
    ec: ExecutionContext
) {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def getFullBlock(
      blockNo: Int,
      traceId: TraceId
  ): Result[BlockError.NotFound, Block.Full] = {
    cardanoDbSyncClient
      .getFullBlock(blockNo)
      .run(traceId)
      .unsafeToFuture()
      .toFutureEither
  }

  def getLatestBlock(
      traceId: TraceId
  ): Result[BlockError.NoneAvailable.type, Block.Canonical] =
    cardanoDbSyncClient.getLatestBlock
      .run(traceId)
      .unsafeToFuture()
      .toFutureEither

  def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): Result[CardanoWalletError, TransactionId] = {
    cardanoWalletApiClient
      .postTransaction(walletId, payments, metadata, passphrase)
      .mapLeft { e =>
        logger.error(s"Could not post the Cardano transaction: ${e.error}")
        CardanoWalletError.fromString(e.error.message, e.error.code)
      }
  }

  def getTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Result[CardanoWalletError, TransactionDetails] = {
    cardanoWalletApiClient
      .getTransaction(walletId, transactionId)
      .mapLeft { e =>
        logger.error(
          s"Could not get Cardano transaction $transactionId: ${e.error}"
        )
        CardanoWalletError.fromString(e.error.message, e.error.code)
      }
  }

  def deleteTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): Result[CardanoWalletError, Unit] = {
    cardanoWalletApiClient
      .deleteTransaction(walletId, transactionId)
      .mapLeft { e =>
        logger.error(
          s"Could not delete Cardano transaction $transactionId: ${e.error}"
        )
        CardanoWalletError.fromString(e.error.message, e.error.code)
      }
  }

  def getWalletDetails(
      walletId: WalletId
  ): Result[CardanoWalletError, WalletDetails] = {
    cardanoWalletApiClient
      .getWallet(walletId)
      .mapLeft { e =>
        logger.error(s"Could not get cardano wallet $walletId: ${e.error}")
        CardanoWalletError.fromString(e.error.message, e.error.code)
      }
  }
}

object CardanoClient {
  type Result[E, A] = FutureEither[E, A]

  case class Config(
      dbSyncConfig: CardanoDbSyncClient.Config,
      cardanoWalletConfig: CardanoWalletApiClient.Config
  )

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def apply(config: Config, logs: Logs[IO, IOWithTraceIdContext])(implicit
      ec: ExecutionContext
  ): Resource[IOWithTraceIdContext, CardanoClient] = {
    CardanoDbSyncClient(config.dbSyncConfig, logs).map(cardanoDbSyncClient =>
      new CardanoClient(
        cardanoDbSyncClient,
        CardanoWalletApiClient(config.cardanoWalletConfig)
      )
    )
  }
}
