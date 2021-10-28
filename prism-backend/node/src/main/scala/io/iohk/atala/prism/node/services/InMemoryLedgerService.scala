package io.iohk.atala.prism.node.services

import cats.{Applicative, Comonad, Functor}
import cats.effect.{MonadThrow, Resource}
import cats.implicits._

import java.time.Instant
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.prism.models.{
  BlockInfo,
  Ledger,
  TransactionDetails,
  TransactionId,
  TransactionInfo,
  TransactionStatus
}
import io.iohk.atala.prism.node.cardano.models.{CardanoWalletError, CardanoWalletErrorCode}
import io.iohk.atala.prism.node.services.logs.UnderlyingLedgerLogs
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.node.{PublicationInfo, UnderlyingLedger}
import io.iohk.atala.prism.protos.node_internal
import tofu.Execute
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}

private final class InMemoryLedgerService[F[_]: MonadThrow](onAtalaObject: AtalaObjectNotificationHandler)(implicit
    ex: Execute[F]
) extends UnderlyingLedger[F] {

  override def getType: Ledger = Ledger.InMemory

  override def publish(
      obj: node_internal.AtalaObject
  ): F[Either[CardanoWalletError, PublicationInfo]] = {
    val publcationInfoF = for {
      objectBytes <- obj.toByteArray.pure[F]
      // Use a hash of the bytes as their in-memory transaction ID
      hash = Sha256.compute(objectBytes)
      transactionId = TransactionId
        .from(hash.getValue)
        .getOrElse(throw new RuntimeException("Unexpected invalid hash"))
      transactionInfo = TransactionInfo(
        transactionId = transactionId,
        ledger = getType,
        // Used for informational purposes only, so fine to hard-code for testing
        block = Some(BlockInfo(number = 1, timestamp = Instant.now(), index = 1))
      )
      _ <- ex.deferFuture(
        onAtalaObject(
          AtalaObjectNotification(obj, transactionInfo)
        )
      )
    } yield PublicationInfo(transactionInfo, TransactionStatus.InLedger)
    publcationInfoF
      .map(publication => publication.asRight[CardanoWalletError])
      .recover { case e =>
        CardanoWalletError(e.getMessage, CardanoWalletErrorCode.UndefinedCardanoWalletError).asLeft[PublicationInfo]
      }
  }

  override def getTransactionDetails(
      transactionId: TransactionId
  ): F[Either[CardanoWalletError, TransactionDetails]] =
    // In-memory transactions are immediately in the ledger
    TransactionDetails(transactionId, TransactionStatus.InLedger).asRight[CardanoWalletError].pure[F]

  override def deleteTransaction(transactionId: TransactionId): F[Either[CardanoWalletError, Unit]] =
    CardanoWalletError(
      "In-memory transactions cannot be deleted",
      CardanoWalletErrorCode.TransactionAlreadyInLedger
    ).asLeft[Unit].pure[F]
}

object InMemoryLedgerService {
  def apply[F[_]: MonadThrow: Execute, R[_]: Functor](
      onAtalaObject: AtalaObjectNotificationHandler,
      logs: Logs[R, F]
  ): R[UnderlyingLedger[F]] =
    for {
      serviceLogs <- logs.service[UnderlyingLedger[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, UnderlyingLedger[F]] = serviceLogs
      val logs: UnderlyingLedger[Mid[F, *]] = new UnderlyingLedgerLogs[F]
      val mid = logs
      mid attach new InMemoryLedgerService[F](onAtalaObject)
    }

  def resource[F[_]: MonadThrow: Execute, R[_]: Applicative](
      onAtalaObject: AtalaObjectNotificationHandler,
      logs: Logs[R, F]
  ): Resource[R, UnderlyingLedger[F]] = Resource.eval(InMemoryLedgerService(onAtalaObject, logs))

  def unsafe[F[_]: MonadThrow: Execute, R[_]: Comonad](
      onAtalaObject: AtalaObjectNotificationHandler,
      logs: Logs[R, F]
  ): UnderlyingLedger[F] = InMemoryLedgerService(onAtalaObject, logs).extract
}
