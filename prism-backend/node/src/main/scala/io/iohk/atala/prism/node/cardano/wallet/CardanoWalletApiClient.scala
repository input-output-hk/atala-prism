package io.iohk.atala.prism.node.cardano.wallet

import cats.Comonad
import cats.effect.{Concurrent, ContextShift, Resource, Sync}
import cats.syntax.comonad._
import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.models.{Lovelace, Payment, TransactionMetadata, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.api.ApiClient
import io.iohk.atala.prism.node.models.WalletDetails

/**
  * Client for the Cardano Wallet API.
  *
  * <p>The client has been trimmed down to only the methods needed. See
  * <a href="https://input-output-hk.github.io/cardano-wallet/api/edge">Cardano Wallet API</a>
  * or its
  * <a href="https://github.com/input-output-hk/cardano-wallet/blob/master/specifications/api/swagger.yaml">spec</a>
  * for the complete API.
  */
trait CardanoWalletApiClient[F[_]] {
  import CardanoWalletApiClient._

  /**
    * Estimate the fee for the given transaction details.
    *
    * @see https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransactionFee
    */
  def estimateTransactionFee(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata]
  ): F[Result[EstimatedFee]]

  /**
    * Post a new transaction and return its ID.
    *
    * @see https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction
    */
  def postTransaction(
      walletId: WalletId,
      payments: List[Payment],
      metadata: Option[TransactionMetadata],
      passphrase: String
  ): F[Result[TransactionId]]

  /**
    * Get the details of the given transaction.
    *
    * @see https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getTransaction
    */
  def getTransaction(walletId: WalletId, transactionId: TransactionId): F[Result[TransactionDetails]]

  /**
    * Forget pending transaction. Importantly, a transaction, when sent, cannot be cancelled.
    * One can only request forgetting about it in order to try spending (concurrently) the same UTxO in another transaction.
    *
    * <p>Note the transaction may still show up later in a block.
    *
    * @see https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteTransaction
    */
  def deleteTransaction(walletId: WalletId, transactionId: TransactionId): F[Result[Unit]]

  /**
    * Get detailed information about the given wallet.
    *
    * @see https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet
    */
  def getWallet(walletId: WalletId): F[Result[WalletDetails]]
}

object CardanoWalletApiClient {

  type Config = ApiClient.Config
  val Config = ApiClient.Config

  def make[F[_]: Concurrent: ContextShift](config: Config): F[CardanoWalletApiClient[F]] = {
    ApiClient.defaultBackend.use(backend => Sync[F].delay(new ApiClient(config)(backend)))
  }

  def makeResource[F[_]: Concurrent: ContextShift](config: Config): Resource[F, ApiClient[F]] = {
    ApiClient.defaultBackend.map(backend => new ApiClient(config)(backend))
  }

  def unsafe[F[_]: Concurrent: ContextShift: Comonad](config: Config): CardanoWalletApiClient[F] =
    make(config).extract

  type Result[A] = Either[ErrorResponse, A]

  final case class CardanoWalletError(code: String, message: String)
  final case class ErrorResponse(requestPath: String, error: CardanoWalletError)

  case class EstimatedFee(min: Lovelace, max: Lovelace)
}
