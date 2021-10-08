package io.iohk.atala.prism.node.cardano

import cats.{Comonad, Functor}
import cats.effect.{Concurrent, ContextShift, IO, Resource}
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.CardanoClient.Result
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.models.WalletDetails
import org.slf4j.{Logger, LoggerFactory}
import tofu.logging.Logs
import tofu.syntax.monadic._
import cats.syntax.either._

import scala.concurrent.ExecutionContext

trait CardanoClient[F[_]] {
  def getFullBlock(blockNo: Int): F[Result[BlockError.NotFound, Block.Full]]

  def getLatestBlock: F[Result[BlockError.NoneAvailable.type, Block.Canonical]]

  def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): F[Result[CardanoWalletError, TransactionId]]

  def getTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): F[Result[CardanoWalletError, TransactionDetails]]

  def deleteTransaction(walletId: WalletId, transactionId: TransactionId): F[Result[CardanoWalletError, Unit]]

  def getWalletDetails(walletId: WalletId): F[Result[CardanoWalletError, WalletDetails]]
}

object CardanoClient {
  type Result[E, A] = Either[E, A]

  case class Config(dbSyncConfig: CardanoDbSyncClient.Config, cardanoWalletConfig: CardanoWalletApiClient.Config)

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def make[I[_]: Comonad, F[_]: Concurrent: ContextShift](
      config: Config,
      logs: Logs[I, F]
  ): Resource[F, CardanoClient[F]] = {
    for {
      cardanoDbSyncClient <- CardanoDbSyncClient[F, I](config.dbSyncConfig, logs)
      cardanoWalletApiClient <- CardanoWalletApiClient.makeResource[F](config.cardanoWalletConfig)
    } yield new CardanoClientImpl(cardanoDbSyncClient, cardanoWalletApiClient)
  }

  def make[F[_]: Functor](
      dbSyncClient: CardanoDbSyncClient[F],
      walletClient: CardanoWalletApiClient[F]
  ): CardanoClient[F] = {
    new CardanoClientImpl(dbSyncClient, walletClient)
  }

  private class CardanoClientImpl[F[_]: Functor](
      cardanoDbSyncClient: CardanoDbSyncClient[F],
      cardanoWalletApiClient: CardanoWalletApiClient[F]
  ) extends CardanoClient[F] {
    private val logger: Logger = LoggerFactory.getLogger(this.getClass)

    def getFullBlock(blockNo: Int): F[Result[BlockError.NotFound, Block.Full]] = {
      cardanoDbSyncClient.getFullBlock(blockNo)
    }

    def getLatestBlock: F[Result[BlockError.NoneAvailable.type, Block.Canonical]] =
      cardanoDbSyncClient.getLatestBlock

    def postTransaction(
        walletId: WalletId,
        payments: List[Payment],
        metadata: Option[TransactionMetadata],
        passphrase: String
    ): F[Result[CardanoWalletError, TransactionId]] = {
      cardanoWalletApiClient
        .postTransaction(walletId, payments, metadata, passphrase)
        .map(_.leftMap { e =>
          logger.error(s"Could not post the Cardano transaction: ${e.error}")
          CardanoWalletError.fromString(e.error.message, e.error.code)
        })
    }

    def getTransaction(
        walletId: WalletId,
        transactionId: TransactionId
    ): F[Result[CardanoWalletError, TransactionDetails]] = {
      cardanoWalletApiClient
        .getTransaction(walletId, transactionId)
        .map(
          _.leftMap { e =>
            logger.error(s"Could not get Cardano transaction $transactionId: ${e.error}")
            CardanoWalletError.fromString(e.error.message, e.error.code)
          }
        )
    }

    def deleteTransaction(
        walletId: WalletId,
        transactionId: TransactionId
    ): F[Result[CardanoWalletError, Unit]] = {
      cardanoWalletApiClient
        .deleteTransaction(walletId, transactionId)
        .map(_.leftMap { e =>
          logger.error(s"Could not delete Cardano transaction $transactionId: ${e.error}")
          CardanoWalletError.fromString(e.error.message, e.error.code)
        })
    }

    def getWalletDetails(walletId: WalletId): F[Result[CardanoWalletError, WalletDetails]] = {
      cardanoWalletApiClient
        .getWallet(walletId)
        .map(_.leftMap { e =>
          logger.error(s"Could not get cardano wallet $walletId: ${e.error}")
          CardanoWalletError.fromString(e.error.message, e.error.code)
        })
    }
  }
}
