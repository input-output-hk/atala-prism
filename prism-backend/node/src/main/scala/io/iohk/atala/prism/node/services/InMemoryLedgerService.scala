package io.iohk.atala.prism.node.services

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
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.node.{PublicationInfo, UnderlyingLedger}
import io.iohk.atala.prism.protos.node_internal

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class InMemoryLedgerService(onAtalaObject: AtalaObjectNotificationHandler)(implicit
    ec: ExecutionContext
) extends UnderlyingLedger {

  override def getType: Ledger = Ledger.InMemory

  override def publish(
      obj: node_internal.AtalaObject
  ): Future[Either[CardanoWalletError, PublicationInfo]] = {
    val publcationInfoF = for {
      objectBytes <- Future.successful(obj.toByteArray)
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
      _ <- onAtalaObject(
        AtalaObjectNotification(obj, transactionInfo)
      )
    } yield PublicationInfo(transactionInfo, TransactionStatus.InLedger)
    publcationInfoF.transform {
      case Success(publication) =>
        Try(publication.asRight[CardanoWalletError])
      case Failure(ex) =>
        Try(
          CardanoWalletError(
            ex.getMessage,
            CardanoWalletErrorCode.UndefinedCardanoWalletError
          ).asLeft[PublicationInfo]
        )
    }
  }

  override def getTransactionDetails(
      transactionId: TransactionId
  ): Future[Either[CardanoWalletError, TransactionDetails]] = {
    // In-memory transactions are immediately in the ledger
    Future.successful(
      TransactionDetails(transactionId, TransactionStatus.InLedger).asRight
    )
  }

  override def deleteTransaction(
      transactionId: TransactionId
  ): Future[Either[CardanoWalletError, Unit]] =
    Future.successful(
      CardanoWalletError(
        "In-memory transactions cannot be deleted",
        CardanoWalletErrorCode.TransactionAlreadyInLedger
      ).asLeft[Unit]
    )
}
