package io.iohk.atala.prism.node.services

import cats.effect.Resource
import cats.implicits._
import cats.{Applicative, Comonad, Functor, MonadThrow}
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.cardano.models.{CardanoWalletError, CardanoWalletErrorCode, Lovelace}
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models.Balance
import io.iohk.atala.prism.node.services.logs.UnderlyingLedgerLogs
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.node.{PublicationInfo, UnderlyingLedger}
import io.iohk.atala.prism.protos.node_models
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}

import java.time.Instant

/** Emulates underlying ledger with in memory data structures instead of blockchain. Useful for debugging and testing.
  */
private final class InMemoryLedgerService[F[_]: MonadThrow](
    onAtalaObject: AtalaObjectNotificationHandler[F]
) extends UnderlyingLedger[F] {

  override def getType: Ledger = Ledger.InMemory

  override def publish(
      obj: node_models.AtalaObject
  ): F[Either[CardanoWalletError, PublicationInfo]] = {
    val publcationInfoF = for {
      objectBytes <- obj.toByteArray.pure[F]
      // Use a hash of the bytes as their in-memory transaction ID
      hash = Sha256Hash.compute(objectBytes)
      transactionId = TransactionId
        .from(hash.bytes)
        .getOrElse(throw new RuntimeException("Unexpected invalid hash"))
      transactionInfo = TransactionInfo(
        transactionId = transactionId,
        ledger = getType,
        // Used for informational purposes only, so fine to hard-code for testing
        block = Some(BlockInfo(number = 1, timestamp = Instant.now(), index = 1))
      )
      _ <- onAtalaObject(
        AtalaObjectNotification(obj, transactionInfo)
      )
    } yield PublicationInfo(transactionInfo, TransactionStatus.InLedger)
    publcationInfoF
      .map(publication => publication.asRight[CardanoWalletError])
      .recover { case e =>
        CardanoWalletError(
          e.getMessage,
          CardanoWalletErrorCode.UndefinedCardanoWalletError
        ).asLeft[PublicationInfo]
      }
  }

  override def getTransactionDetails(
      transactionId: TransactionId
  ): F[Either[CardanoWalletError, TransactionDetails]] =
    // In-memory transactions are immediately in the ledger
    TransactionDetails(transactionId, TransactionStatus.InLedger)
      .asRight[CardanoWalletError]
      .pure[F]

  override def deleteTransaction(
      transactionId: TransactionId
  ): F[Either[CardanoWalletError, Unit]] =
    CardanoWalletError(
      "In-memory transactions cannot be deleted",
      CardanoWalletErrorCode.TransactionAlreadyInLedger
    ).asLeft[Unit].pure[F]

  override def getWalletBalance: F[Either[CardanoWalletError, Balance]] =
    Balance(Lovelace(0)).asRight[CardanoWalletError].pure[F]
}

object InMemoryLedgerService {
  def apply[F[_]: MonadThrow, R[_]: Functor](
      onAtalaObject: AtalaObjectNotificationHandler[F],
      logs: Logs[R, F]
  ): R[UnderlyingLedger[F]] =
    for {
      serviceLogs <- logs.service[UnderlyingLedger[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, UnderlyingLedger[F]] =
        serviceLogs
      val logs: UnderlyingLedger[Mid[F, *]] = new UnderlyingLedgerLogs[F]
      val mid = logs
      mid attach new InMemoryLedgerService[F](onAtalaObject)
    }

  def resource[F[_]: MonadThrow, R[_]: Applicative](
      onAtalaObject: AtalaObjectNotificationHandler[F],
      logs: Logs[R, F]
  ): Resource[R, UnderlyingLedger[F]] =
    Resource.eval(InMemoryLedgerService(onAtalaObject, logs))

  def unsafe[F[_]: MonadThrow, R[_]: Comonad](
      onAtalaObject: AtalaObjectNotificationHandler[F],
      logs: Logs[R, F]
  ): UnderlyingLedger[F] = InMemoryLedgerService(onAtalaObject, logs).extract
}
