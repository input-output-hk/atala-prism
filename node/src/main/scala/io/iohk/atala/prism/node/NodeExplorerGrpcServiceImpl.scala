package io.iohk.atala.prism.node

import cats.effect.unsafe.IORuntime
import cats.syntax.traverse._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.node.auth.WhitelistedAuthenticatorF
import io.iohk.atala.prism.node.auth.grpc.GrpcAuthenticationHeaderParser.grpcHeader
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.logging.TraceId
import io.iohk.atala.prism.node.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.metrics.RequestMeasureUtil
import io.iohk.atala.prism.node.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.node.models.TransactionId
import io.iohk.atala.prism.node.NodeExplorerGrpcServiceImpl.{countAndThrowNodeError, serviceName}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.metrics.StatisticsCounters
import io.iohk.atala.prism.node.services.{NodeExplorerService, StatisticsService}
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.GetScheduledOperationsRequest.OperationType.{
  AnyOperationType,
  CreateDidOperationOperationType,
  ProtocolVersionUpdateOperationType,
  UpdateDidOperationOperationType
}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.node.tracing.Tracing.trace
import io.iohk.atala.prism.node.utils.FutureEither.FutureEitherOps
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}

class NodeExplorerGrpcServiceImpl(
    authenticator: WhitelistedAuthenticatorF[IOWithTraceIdContext],
    nodeExplorerService: NodeExplorerService[IOWithTraceIdContext],
    statisticsService: StatisticsService[IOWithTraceIdContext],
    didWhitelist: Set[DID]
)(implicit
    ec: ExecutionContext,
    runtime: IORuntime
) extends node_api.NodeExplorerServiceGrpc.NodeExplorerService {

  override def getAvailableMetrics(request: GetAvailableMetricsRequest): Future[GetAvailableMetricsResponse] = {
    val methodName = "getAvailableMetrics"

    measureRequestFuture(serviceName, methodName) {
      trace { traceId =>
        whitelistedGuard(traceId, methodName, request)
          .flatMap(_ =>
            Future.successful(
              GetAvailableMetricsResponse(StatisticsCounters.MetricCounter.lowerCaseNamesToValuesMap.keys.toList)
            )
          )
      }
    }
  }

  override def getNodeStatistics(request: GetNodeStatisticsRequest): Future[GetNodeStatisticsResponse] = {
    val methodName = "getNodeStatistics"

    measureRequestFuture(serviceName, methodName) {
      trace { traceId =>
        val query = for {
          metricsE <- request.metrics.traverse(statisticsService.retrieveMetric).run(traceId)
          metrics = metricsE.zip(request.metrics).map { case (metricE, _) =>
            metricE.fold(
              _ => 0.0,
              value => value.toDouble
            )
          }
        } yield GetNodeStatisticsResponse(metrics)

        whitelistedGuard(traceId, methodName, request)
          .flatMap(_ => query.unsafeToFuture())
      }
    }
  }

  /** * PUBLIC
    *
    * Return a list of scheduled but unconfirmed operations.
    */
  override def getScheduledOperations(
      request: GetScheduledOperationsRequest
  ): Future[GetScheduledOperationsResponse] = {
    val methodName = "getScheduledOperations"

    measureRequestFuture(serviceName, methodName)(
      trace { traceId =>
        val query =
          for {
            operationsE <- nodeExplorerService.getScheduledAtalaOperations
            operations = operationsE.fold(err => countAndThrowNodeError(methodName, err), info => info)
          } yield node_api
            .GetScheduledOperationsResponse()
            .withScheduledOperations(
              operations.filter(o =>
                request.operationsType match {
                  case AnyOperationType => true
                  case CreateDidOperationOperationType =>
                    o.operation.isDefined && o.operation.get.operation.isCreateDid
                  case UpdateDidOperationOperationType =>
                    o.operation.isDefined && o.operation.get.operation.isUpdateDid
                  case ProtocolVersionUpdateOperationType =>
                    o.operation.isDefined && o.operation.get.operation.isProtocolVersionUpdate
                  case _ => false
                }
              )
            )

        whitelistedGuard(traceId, methodName, request)
          .flatMap(_ => query.run(traceId).unsafeToFuture())
      }
    )
  }

  /** * PUBLIC
    *
    * Return a list of wallet transactions.
    */
  override def getWalletTransactionsPaginated(
      request: GetWalletTransactionsRequest
  ): Future[GetWalletTransactionsResponse] = {
    val methodName = "getWalletTransactionsPaginated"

    measureRequestFuture(serviceName, methodName)(
      trace { traceId =>
        val query =
          for {
            transactionsE <- nodeExplorerService
              .getWalletTransactions(request.state, TransactionId.from(request.lastSeenTransactionId), request.limit)
            transactions = transactionsE.fold(err => countAndThrowNodeError(methodName, err), info => info)
          } yield node_api
            .GetWalletTransactionsResponse()
            .withTransactions(transactions.map(_.toProto))

        whitelistedGuard(traceId, methodName, request)
          .flatMap(_ => query.run(traceId).unsafeToFuture())
      }
    )
  }

  /** * PUBLIC
    *
    * Return the Node Wallet Balance
    */
  override def getWalletBalance(request: GetWalletBalanceRequest): Future[GetWalletBalanceResponse] = {
    val methodName = "getWalletBalance"

    measureRequestFuture(serviceName, methodName)(
      trace { traceId =>
        val query =
          for {
            maybeWalletBalance <- nodeExplorerService.getWalletBalance
          } yield maybeWalletBalance.fold(
            throw _,
            walletBalance =>
              node_api
                .GetWalletBalanceResponse()
                .withBalance(
                  ByteString.copyFrom(walletBalance.available.toByteArray)
                )
          )

        whitelistedGuard(traceId, methodName, request)
          .flatMap(_ => query.run(traceId).unsafeToFuture())
      }
    )
  }

  private def whitelistedGuard[R <: GeneratedMessage](traceId: TraceId, methodName: String, request: R): Future[DID] =
    grpcHeader { header =>
      authenticator
        .whitelistedDid(didWhitelist, methodName, request, header)
        .run(traceId)
        .unsafeToFuture()
        .toFutureEither
        .toFuture(_.toStatus.asRuntimeException())
    }
}

object NodeExplorerGrpcServiceImpl {
  val serviceName = "node-explorer-service"

  def countAndThrowNodeError(methodName: String, error: NodeError): Nothing =
    RequestMeasureUtil.countAndThrowNodeError(serviceName)(methodName, error.toStatus)
}
