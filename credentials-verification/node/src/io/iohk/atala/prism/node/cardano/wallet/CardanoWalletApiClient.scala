package io.iohk.atala.prism.node.cardano.wallet

import io.iohk.atala.prism.models.TransactionId
import io.iohk.atala.prism.node.cardano.models.{Payment, WalletId}
import io.iohk.atala.prism.node.cardano.wallet.api.ApiClient
import io.iohk.atala.prism.utils.FutureEither

import scala.concurrent.ExecutionContext

/**
  * Client for the Cardano Wallet API.
  *
  * <p>The client has been trimmed down to only the methods needed. See
  * <a href="https://input-output-hk.github.io/cardano-wallet/api/edge">Cardano Wallet API</a>
  * or its
  * <a href="https://github.com/input-output-hk/cardano-wallet/blob/master/specifications/api/swagger.yaml">spec</a>
  * for the complete API.
  */
trait CardanoWalletApiClient {
  import CardanoWalletApiClient._

  /**
    * Post a new transaction and return its ID.
    *
    * @see https://input-output-hk.github.io/cardano-wallet/api/edge/#operation/postTransaction
    */
  def postTransaction(walletId: WalletId, payments: List[Payment], passphrase: String): Result[TransactionId]
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
}
