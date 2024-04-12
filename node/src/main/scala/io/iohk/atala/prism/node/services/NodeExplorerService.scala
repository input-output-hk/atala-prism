package io.iohk.atala.prism.node.services

import cats.effect.Resource
import cats.implicits._
import cats.{Comonad, Functor, MonadThrow}
import derevo.derive
import derevo.tagless.applyK
import io.iohk.atala.prism.node.models.{TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.UnderlyingLedger
import io.iohk.atala.prism.node.cardano.models.CardanoWalletError
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.InvalidArgument
import io.iohk.atala.prism.node.models.Balance
import io.iohk.atala.prism.node.services.logs.NodeExplorerServiceLogs
import io.iohk.atala.prism.protos.node_api.GetWalletTransactionsRequest
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import io.iohk.atala.prism.protos.{node_api, node_models}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}

/** Implements logic for RPC calls in Node Explorer gRPC Server.
  */
@derive(applyK)
trait NodeExplorerService[F[_]] {

  def getScheduledAtalaOperations: F[Either[NodeError, List[node_models.SignedAtalaOperation]]]

  def getWalletTransactions(
      transactionType: node_api.GetWalletTransactionsRequest.TransactionState,
      lastSeenTransactionId: Option[TransactionId],
      limit: Int = 50
  ): F[Either[NodeError, List[TransactionInfo]]]

  def getWalletBalance: F[Either[CardanoWalletError, Balance]]
}

private final class NodeExplorerServiceImpl[F[_]: MonadThrow](
    underlyingLedger: UnderlyingLedger[F],
    objectManagement: ObjectManagementService[F]
) extends NodeExplorerService[F] {

  override def getScheduledAtalaOperations: F[Either[NodeError, List[SignedAtalaOperation]]] =
    objectManagement.getScheduledAtalaObjects
      .map(_.map(_.flatMap(obj => obj.getAtalaBlock.map(_.operations).getOrElse(Seq()))))

  override def getWalletTransactions(
      transactionType: GetWalletTransactionsRequest.TransactionState,
      lastSeenTransactionId: Option[TransactionId],
      limit: Int
  ): F[Either[NodeError, List[TransactionInfo]]] = {
    transactionType match {
      case GetWalletTransactionsRequest.TransactionState.Ongoing =>
        objectManagement.getUnconfirmedTransactions(lastSeenTransactionId, limit)
      case GetWalletTransactionsRequest.TransactionState.Confirmed =>
        objectManagement.getConfirmedTransactions(lastSeenTransactionId, limit)
      case _ =>
        Either
          .left[NodeError, List[TransactionInfo]](InvalidArgument("Unrecognized transaction type"): NodeError)
          .pure[F]
    }
  }

  override def getWalletBalance: F[Either[CardanoWalletError, Balance]] =
    underlyingLedger.getWalletBalance
}

object NodeExplorerService {

  def make[I[_]: Functor, F[_]: MonadThrow](
      underlyingLedger: UnderlyingLedger[F],
      objectManagement: ObjectManagementService[F],
      logs: Logs[I, F]
  ): I[NodeExplorerService[F]] = {
    for {
      serviceLogs <- logs.service[NodeExplorerService[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, NodeExplorerService[F]] = serviceLogs
      val logs: NodeExplorerService[Mid[F, *]] = new NodeExplorerServiceLogs[F]
      val mid: NodeExplorerService[Mid[F, *]] = logs
      mid attach new NodeExplorerServiceImpl[F](
        underlyingLedger,
        objectManagement
      )
    }
  }

  def resource[I[_]: Comonad, F[_]: MonadThrow](
      underlyingLedger: UnderlyingLedger[F],
      objectManagement: ObjectManagementService[F],
      logs: Logs[I, F]
  ): Resource[I, NodeExplorerService[F]] = Resource.eval(
    make(underlyingLedger, objectManagement, logs)
  )

  def unsafe[I[_]: Comonad, F[_]: MonadThrow](
      underlyingLedger: UnderlyingLedger[F],
      objectManagement: ObjectManagementService[F],
      logs: Logs[I, F]
  ): NodeExplorerService[F] =
    make(
      underlyingLedger,
      objectManagement,
      logs
    ).extract
}
