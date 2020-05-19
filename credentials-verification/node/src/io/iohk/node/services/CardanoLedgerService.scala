package io.iohk.node.services

import java.time.Instant

import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.utils.FutureEither
import io.iohk.node.AtalaReferenceLedger
import io.iohk.node.cardano.CardanoClient
import io.iohk.node.cardano.models.{Address, Lovelace, Payment, WalletId}
import io.iohk.node.services.models.{AtalaObjectUpdate, ObjectHandler}

import scala.concurrent.{ExecutionContext, Future}

class CardanoLedgerService private[services] (
    walletId: WalletId,
    walletPassphrase: String,
    paymentAddress: Address,
    cardanoClient: CardanoClient,
    onNewObject: ObjectHandler
)(implicit
    ec: ExecutionContext
) extends AtalaReferenceLedger {

  override def supportsOnChainData: Boolean = false

  override def publishReference(ref: SHA256Digest): Future[Unit] = {
    // TODO: Send `ref` as metadata
    cardanoClient
      .postTransaction(walletId, List(Payment(paymentAddress, Lovelace(1))), walletPassphrase)
      .value
      .map {
        // TODO: Wait 31 (configurable) confirmations before notifying
        case Right(_) => onNewObject(AtalaObjectUpdate.Reference(ref), Instant.now())
        case Left(error) =>
          throw new RuntimeException(s"FATAL: Error during publishing reference: ${error}")
      }
  }

  override def publishObject(bytes: Array[Byte]): Future[Unit] = {
    throw new NotImplementedError("Publishing whole objects not implemented for Cardano ledger")
  }
}

object CardanoLedgerService {

  type Result[E, A] = FutureEither[E, A]

  case class Config(
      walletId: String,
      walletPassphrase: String,
      paymentAddress: String,
      cardanoClientConfig: CardanoClient.Config
  )

  def apply(config: Config, onNewObject: ObjectHandler)(implicit ec: ExecutionContext): CardanoLedgerService = {
    val walletId = WalletId
      .from(config.walletId)
      .getOrElse(throw new IllegalArgumentException(s"Wallet ID ${config.walletId} is invalid"))
    val walletPassphrase = config.walletPassphrase
    val paymentAddress = Address(config.paymentAddress)
    val cardanoClient = CardanoClient(config.cardanoClientConfig)

    new CardanoLedgerService(
      walletId,
      walletPassphrase,
      paymentAddress,
      cardanoClient,
      onNewObject
    )
  }
}
