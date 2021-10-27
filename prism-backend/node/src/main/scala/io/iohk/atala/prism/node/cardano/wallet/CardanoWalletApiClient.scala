package io.iohk.atala.prism.node.cardano.wallet

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.syntax.comonad._
import cats.syntax.functor._
import cats.{Applicative, Comonad, Functor}
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.models.{Lovelace, Payment, TransactionMetadata, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.api.ApiClient
import io.iohk.atala.prism.node.cardano.wallet.logs.CardanoWalletApiClientLogs
import io.iohk.atala.prism.node.cardano.wallet.metrics.CardanoWalletApiClientMetrics
import io.iohk.atala.prism.node.models.WalletDetails
import tofu.higherKind.Mid
import tofu.logging.derivation.loggable
import tofu.logging.{DictLoggable, LogRenderer, Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

/** Client for the Cardano Wallet API.
  *
  * <p>The client has been trimmed down to only the methods needed. See <a
  * href="https://input-output-hk.github.io/cardano-wallet/api/edge">Cardano Wallet API</a> or its <a
  * href="https://github.com/input-output-hk/cardano-wallet/blob/master/specifications/api/swagger.yaml">spec</a> for
  * the complete API.
  */
@derive(applyK)
trait CardanoWalletApiClient[F[_]] {
  import CardanoWalletApiClient._

  /** Estimate the fee for the given transaction details.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransactionFee
    */
  def estimateTransactionFee(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata]
  ): F[Result[EstimatedFee]]

  /** Post a new transaction and return its ID.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction
    */
  def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): F[Result[TransactionId]]

  /** Get the details of the given transaction.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getTransaction
    */
  def getTransaction(walletId: WalletId, transactionId: TransactionId): F[Result[TransactionDetails]]

  /** Forget pending transaction. Importantly, a transaction, when sent, cannot be cancelled. One can only request
    * forgetting about it in order to try spending (concurrently) the same UTxO in another transaction.
    *
    * <p>Note the transaction may still show up later in a block.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteTransaction
    */
  def deleteTransaction(walletId: WalletId, transactionId: TransactionId): F[Result[Unit]]

  /** Get detailed information about the given wallet.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet
    */
  def getWallet(walletId: WalletId): F[Result[WalletDetails]]
}

object CardanoWalletApiClient {

  type Config = ApiClient.Config
  val Config = ApiClient.Config
  def make[F[_]: TimeMeasureMetric: Concurrent: ContextShift, I[_]: Functor](
      config: Config,
      logs: Logs[I, F]
  ): I[F[CardanoWalletApiClient[F]]] = for {
    logs <- logs.service[CardanoWalletApiClient[F]]
  } yield {
    implicit val implicitLogs: ServiceLogging[F, CardanoWalletApiClient[F]] = logs
    ApiClient.defaultBackend.use { backend =>
      val logging: CardanoWalletApiClient[Mid[F, *]] = new CardanoWalletApiClientLogs[F]
      val metrics: CardanoWalletApiClient[Mid[F, *]] = new CardanoWalletApiClientMetrics[F]
      val client: CardanoWalletApiClient[F] = new ApiClient(config, backend)
      val mid = metrics |+| logging
      Applicative[F].pure(mid attach client)
    }
  }

  def makeResource[F[_]: TimeMeasureMetric: Concurrent: ContextShift, I[_]: Comonad](
      config: Config,
      logs: Logs[I, F]
  ): Resource[F, CardanoWalletApiClient[F]] = {
    logs
      .service[CardanoWalletApiClient[F]]
      .map { implicit logging =>
        ApiClient.defaultBackend.map { backend =>
          val logs: CardanoWalletApiClient[Mid[F, *]] = new CardanoWalletApiClientLogs[F]
          val metrics: CardanoWalletApiClient[Mid[F, *]] = new CardanoWalletApiClientMetrics[F]
          val mid = metrics |+| logs
          mid attach new ApiClient(config, backend)
        }
      }
      .extract
  }

  def unsafe[F[_]: TimeMeasureMetric: Concurrent: ContextShift: Comonad, I[_]: Comonad](
      config: Config,
      logs: Logs[I, F]
  ): CardanoWalletApiClient[F] =
    make[F, I](config, logs).extract.extract

  type Result[A] = Either[ErrorResponse, A]

  @derive(loggable)
  final case class CardanoWalletError(code: String, message: String)
  @derive(loggable)
  final case class ErrorResponse(requestPath: String, error: CardanoWalletError)

  case class EstimatedFee(min: Lovelace, max: Lovelace)

  object EstimatedFee {
    implicit val estimatedFeeLoggable: DictLoggable[EstimatedFee] = new DictLoggable[EstimatedFee] {
      override def fields[I, V, R, S](a: EstimatedFee, i: I)(implicit r: LogRenderer[I, V, R, S]): R =
        r.addBigInt("min", a.min, i) |+| r.addBigInt("max", a.max, i)

      override def logShow(a: EstimatedFee): String = s"EstimatedFee{min=${a.min} max=${a.max}}"
    }
  }
}
