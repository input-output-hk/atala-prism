package io.iohk.atala.prism.node.cardano

import cats.{Comonad, Functor}
import cats.effect.{Concurrent, ContextShift, IO, MonadThrow, Resource}
import cats.syntax.comonad._
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.atala.prism.node.cardano.models._
import io.iohk.atala.prism.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.atala.prism.node.models.WalletDetails
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monadic._
import cats.syntax.either._
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.node.cardano.logs.CardanoClientLogs
import tofu.higherKind.Mid
import io.iohk.atala.prism.metrics.TimeMeasureMetric

import scala.concurrent.ExecutionContext

@derive(applyK)
trait CardanoClient[F[_]] {
  def getFullBlock(blockNo: Int): F[Either[BlockError.NotFound, Block.Full]]

  def getLatestBlock: F[Either[BlockError.NoneAvailable.type, Block.Canonical]]

  def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): F[Either[CardanoWalletError, TransactionId]]

  def getTransaction(
      walletId: WalletId,
      transactionId: TransactionId
  ): F[Either[CardanoWalletError, TransactionDetails]]

  def deleteTransaction(walletId: WalletId, transactionId: TransactionId): F[Either[CardanoWalletError, Unit]]

  def getWalletDetails(walletId: WalletId): F[Either[CardanoWalletError, WalletDetails]]
}

object CardanoClient {

  case class Config(dbSyncConfig: CardanoDbSyncClient.Config, cardanoWalletConfig: CardanoWalletApiClient.Config)

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  def make[I[_]: Functor, F[_]: MonadThrow](
      cardanoDbSyncClient: CardanoDbSyncClient[F],
      cardanoWalletApiClient: CardanoWalletApiClient[F],
      logs: Logs[I, F]
  ): I[CardanoClient[F]] =
    for {
      serviceLogs <- logs.service[CardanoClient[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CardanoClient[F]] = serviceLogs
      val logs: CardanoClient[Mid[F, *]] = new CardanoClientLogs[F]
      val mid = logs
      mid attach new CardanoClientImpl(cardanoDbSyncClient, cardanoWalletApiClient)
    }

  def makeResource[I[_]: Comonad, F[_]: TimeMeasureMetric: Concurrent: ContextShift](
      config: Config,
      logs: Logs[I, F]
  ): Resource[F, CardanoClient[F]] =
    for {
      cardanoDbSyncClient <- CardanoDbSyncClient[F, I](config.dbSyncConfig, logs)
      cardanoWalletApiClient <- CardanoWalletApiClient.makeResource[F, I](config.cardanoWalletConfig, logs)
    } yield CardanoClient.make[I, F](cardanoDbSyncClient, cardanoWalletApiClient, logs).extract

  def makeUnsafe[F[_]: Functor](
      dbSyncClient: CardanoDbSyncClient[F],
      walletClient: CardanoWalletApiClient[F]
  ): CardanoClient[F] = {
    new CardanoClientImpl(dbSyncClient, walletClient)
  }

  private class CardanoClientImpl[F[_]: Functor](
      cardanoDbSyncClient: CardanoDbSyncClient[F],
      cardanoWalletApiClient: CardanoWalletApiClient[F]
  ) extends CardanoClient[F] {

    def getFullBlock(blockNo: Int): F[Either[BlockError.NotFound, Block.Full]] = {
      cardanoDbSyncClient.getFullBlock(blockNo)
    }

    def getLatestBlock: F[Either[BlockError.NoneAvailable.type, Block.Canonical]] =
      cardanoDbSyncClient.getLatestBlock

    def postTransaction(
        walletId: WalletId,
        payments: List[Payment],
        metadata: Option[TransactionMetadata],
        passphrase: String
    ): F[Either[CardanoWalletError, TransactionId]] = {
      cardanoWalletApiClient
        .postTransaction(walletId, payments, metadata, passphrase)
        .map(_.leftMap { e =>
          CardanoWalletError.fromString(e.error.message, e.error.code)
        })
    }

    def getTransaction(
        walletId: WalletId,
        transactionId: TransactionId
    ): F[Either[CardanoWalletError, TransactionDetails]] = {
      cardanoWalletApiClient
        .getTransaction(walletId, transactionId)
        .map(
          _.leftMap { e =>
            CardanoWalletError.fromString(e.error.message, e.error.code)
          }
        )
    }

    def deleteTransaction(
        walletId: WalletId,
        transactionId: TransactionId
    ): F[Either[CardanoWalletError, Unit]] = {
      cardanoWalletApiClient
        .deleteTransaction(walletId, transactionId)
        .map(_.leftMap { e =>
          CardanoWalletError.fromString(e.error.message, e.error.code)
        })
    }

    def getWalletDetails(walletId: WalletId): F[Either[CardanoWalletError, WalletDetails]] = {
      cardanoWalletApiClient
        .getWallet(walletId)
        .map(_.leftMap { e =>
          CardanoWalletError.fromString(e.error.message, e.error.code)
        })
    }
  }
}
