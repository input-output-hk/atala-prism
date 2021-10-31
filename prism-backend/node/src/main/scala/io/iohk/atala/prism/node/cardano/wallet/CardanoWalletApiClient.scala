package io.iohk.atala.prism.node.cardano.wallet

import io.iohk.atala.prism.models.{TransactionDetails, TransactionId}
import io.iohk.atala.prism.node.cardano.models.{Lovelace, Payment, TransactionMetadata, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.api.ApiClient
import io.iohk.atala.prism.node.models.WalletDetails
import io.iohk.atala.prism.utils.FutureEither

import scala.concurrent.ExecutionContext

/** Client for the Cardano Wallet API.
  *
  * <p>The client has been trimmed down to only the methods needed. See <a
  * href="https://input-output-hk.github.io/cardano-wallet/api/edge">Cardano Wallet API</a> or its <a
  * href="https://github.com/input-output-hk/cardano-wallet/blob/master/specifications/api/swagger.yaml">spec</a> for
  * the complete API.
  */
trait CardanoWalletApiClient {
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
  ): Result[EstimatedFee]

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
  ): Result[TransactionId]

  /** Get the details of the given transaction.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getTransaction
    */
  def getTransaction(walletId: WalletId, transactionId: TransactionId): Result[TransactionDetails]

  /** Forget pending transaction. Importantly, a transaction, when sent, cannot be cancelled. One can only request
    * forgetting about it in order to try spending (concurrently) the same UTxO in another transaction.
    *
    * <p>Note the transaction may still show up later in a block.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/deleteTransaction
    */
  def deleteTransaction(walletId: WalletId, transactionId: TransactionId): Result[Unit]

  /** Get detailed information about the given wallet.
    *
    * @see
    *   https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/getWallet
    */
  def getWallet(walletId: WalletId): Result[WalletDetails]
}

object CardanoWalletApiClient {

  type Config = ApiClient.Config
  val Config = ApiClient.Config

  def apply(config: Config)(implicit ec: ExecutionContext): CardanoWalletApiClient = {
    new ApiClient(config)(ApiClient.DefaultBackend, ec)
  }

  type Result[A] = FutureEither[ErrorResponse, A]

  final case class CardanoWalletError(code: String, message: String)
  final case class ErrorResponse(requestPath: String, error: CardanoWalletError)

  case class EstimatedFee(min: Lovelace, max: Lovelace)
}
