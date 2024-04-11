package io.iohk.atala.prism.node.services.logs

import cats.MonadThrow
import cats.syntax.applicativeError._
import cats.syntax.apply._
import cats.syntax.flatMap._
import io.iohk.atala.prism.node.models.{TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.Balance
import io.iohk.atala.prism.node.services.NodeExplorerService
import io.iohk.atala.prism.protos.node_api.GetWalletTransactionsRequest
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import tofu.higherKind.Mid
import tofu.logging.ServiceLogging
import tofu.syntax.logging._

class NodeExplorerServiceLogs[F[_]: ServiceLogging[*[_], NodeExplorerService[F]]: MonadThrow]
    extends NodeExplorerService[Mid[F, *]] {
  override def getScheduledAtalaOperations: Mid[F, Either[NodeError, List[SignedAtalaOperation]]] = { in =>
    val description = s"getting scheduled Atala operations"
    info"$description" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while $description: $err",
          ops => info"$description - successfully done, number of operations ${ops.size}"
        )
      )
      .onError(errorCause"encountered an error while $description" (_))
  }

  override def getWalletTransactions(
      transactionType: GetWalletTransactionsRequest.TransactionState,
      lastSeenTransactionId: Option[TransactionId],
      limit: Int
  ): Mid[F, Either[NodeError, List[TransactionInfo]]] = { in =>
    val description = s"getting wallet transactions"
    info"$description" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while $description: $err",
          txs => info"$description - successfully done, number of transactions ${txs.size}"
        )
      )
      .onError(errorCause"encountered an error while $description" (_))
  }

  override def getWalletBalance: Mid[F, Either[CardanoWalletError, Balance]] = { in =>
    val description = s"getting wallet balance"
    info"$description" *> in
      .flatTap(
        _.fold(
          err => error"encountered an error while $description: $err",
          res => info"$description - done: $res"
        )
      )
      .onError(errorCause"encountered an error while $description" (_))
  }
}
