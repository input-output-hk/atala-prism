package io.iohk.atala.prism.node

import cats.effect.unsafe.IORuntime
import com.google.protobuf.ByteString
import io.grpc.Status
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.node.logging.TraceId
import io.iohk.atala.prism.node.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.metrics.RequestMeasureUtil
import io.iohk.atala.prism.node.metrics.RequestMeasureUtil.measureRequestFuture
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.AtalaObjectTransactionSubmissionStatus.InLedger
import io.iohk.atala.prism.node.models.{
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationInfo,
  AtalaOperationStatus
}
import io.iohk.atala.prism.node.operations.protocolVersion.SUPPORTED_VERSION
import io.iohk.atala.prism.node.services._
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.{common_models, node_api}
import io.iohk.atala.prism.node.tracing.Tracing._
import io.iohk.atala.prism.node.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class NodeGrpcServiceImpl(
    nodeService: NodeService[IOWithTraceIdContext]
)(implicit
    ec: ExecutionContext,
    runtime: IORuntime
) extends node_api.NodeServiceGrpc.NodeService {

  import NodeGrpcServiceImpl._

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(
      request: HealthCheckRequest
  ): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse())

  override def getDidDocument(
      request: node_api.GetDidDocumentRequest
  ): Future[node_api.GetDidDocumentResponse] = {
    val methodName = "getDidDocument"

    measureRequestFuture(serviceName, methodName) {
      trace { traceId =>
        getDidDocument(request.did, methodName, traceId)
      }
    }

  }

  override def scheduleOperations(
      request: node_api.ScheduleOperationsRequest
  ): Future[node_api.ScheduleOperationsResponse] = {
    val methodName = "scheduleOperations"

    measureRequestFuture(serviceName, methodName) {
      trace { traceId =>
        val query = for {
          operationOutputsE <- nodeService.parseOperations(request.signedOperations)
          operationOutputs = operationOutputsE.fold(err => countAndThrowNodeError(methodName, err), outs => outs)

          operationIds <- nodeService.scheduleAtalaOperations(request.signedOperations: _*)
          outputsWithOperationIds = operationOutputs.zip(operationIds).map {
            case (out, Right(opId)) =>
              out.withOperationId(opId.toProtoByteString)
            case (out, Left(err)) =>
              out.withError(err.toString)
          }
        } yield node_api.ScheduleOperationsResponse().withOutputs(outputsWithOperationIds)
        query.run(traceId).unsafeToFuture()
      }
    }
  }

  override def getOperationInfo(
      request: node_api.GetOperationInfoRequest
  ): Future[node_api.GetOperationInfoResponse] = {
    val methodName = "getOperationInfo"

    measureRequestFuture(serviceName, methodName) {
      trace { traceId =>
        val query = for {
          operationInfoE <- nodeService.getOperationInfo(request.operationId)
          operationInfo = operationInfoE.fold(err => countAndThrowNodeError(methodName, err), info => info)
          maybeOperationInfo = operationInfo.maybeOperationInfo
        } yield {
          val (operationStatus, operationStatusDetails) = maybeOperationInfo
            .fold[(common_models.OperationStatus, String)](
              (common_models.OperationStatus.UNKNOWN_OPERATION, "")
            ) { case AtalaOperationInfo(_, _, opStatus, opStatusDetails, maybeTxStatus, _) =>
              (evalOperationStatus(opStatus, maybeTxStatus), opStatusDetails)
            }
          val response = node_api
            .GetOperationInfoResponse()
            .withOperationStatus(operationStatus)
            .withDetails(operationStatusDetails)
            .withLastSyncedBlockTimestamp(operationInfo.lastSyncedTimestamp.toProtoTimestamp)
          val responseWithTransactionId = maybeOperationInfo
            .flatMap(_.transactionId)
            .map(_.toString)
            .fold(response)(response.withTransactionId)

          responseWithTransactionId
        }
        query.run(traceId).unsafeToFuture()
      }
    }
  }

  private def getDidDocument(
      didRequestStr: String,
      methodName: String,
      traceId: TraceId
  ): Future[GetDidDocumentResponse] = {
    val query = for {
      didE <- nodeService.getDidDocumentByDid(didRequestStr)
    } yield didE.fold(
      countAndThrowGetDidDocumentError(methodName, didRequestStr, _),
      didData =>
        node_api
          .GetDidDocumentResponse(document = didData.maybeData)
          .withLastUpdateOperation(
            didData.maybeOperation.map(a => ByteString.copyFrom(a.getValue)).getOrElse(ByteString.EMPTY)
          )
          .withLastSyncedBlockTimestamp(didData.lastSyncedTimeStamp.toProtoTimestamp)
    )
    query.run(traceId).unsafeToFuture()
  }

  // This method returns statuses only for operations
  // which the node sent to the Cardano chain itself.
  private def evalOperationStatus(
      opStatus: AtalaOperationStatus,
      maybeTxStatus: Option[AtalaObjectTransactionSubmissionStatus]
  ): common_models.OperationStatus = {
    (opStatus, maybeTxStatus) match {
      case (AtalaOperationStatus.RECEIVED, None) =>
        common_models.OperationStatus.PENDING_SUBMISSION
      case (AtalaOperationStatus.RECEIVED, Some(_)) =>
        common_models.OperationStatus.AWAIT_CONFIRMATION
      case (AtalaOperationStatus.APPLIED, None) => // See ATL-642
        logger.warn(
          "The operation seems to be in a transition state into the APPLIED status. " +
            "(Database is eventually consistent but should not take more than a few milliseconds)"
        )
        common_models.OperationStatus.AWAIT_CONFIRMATION // This is the previous (consistent) state
      case (AtalaOperationStatus.REJECTED, None) => // See ATL-1267
        logger.warn(
          "The operation seems to be in a transition state into the REJECTED status. " +
            "(Database is eventually consistent but should not take more than a few milliseconds)"
        )
        common_models.OperationStatus.AWAIT_CONFIRMATION // This is the previous (consistent) state
      case (AtalaOperationStatus.APPLIED, Some(InLedger)) =>
        common_models.OperationStatus.CONFIRMED_AND_APPLIED
      case (AtalaOperationStatus.REJECTED, Some(InLedger)) =>
        common_models.OperationStatus.CONFIRMED_AND_REJECTED
      case _ =>
        throw new RuntimeException(
          s"Unknown state of the operation: (operationStatus = $opStatus, transactionStatus = $maybeTxStatus)"
        )
    }
  }

  override def getLastSyncedBlockTimestamp(
      request: node_api.GetLastSyncedBlockTimestampRequest
  ): Future[node_api.GetLastSyncedBlockTimestampResponse] = {
    val methodName = "lastSyncedTimestamp"
    measureRequestFuture(serviceName, methodName)(
      trace { traceId =>
        val query =
          for {
            lastSyncedBlockTimestamp <- nodeService.getLastSyncedTimestamp
          } yield node_api
            .GetLastSyncedBlockTimestampResponse()
            .withLastSyncedBlockTimestamp(
              lastSyncedBlockTimestamp.toProtoTimestamp
            )
        query.run(traceId).unsafeToFuture()
      }
    )
  }

  override def getNodeBuildInfo(
      request: node_api.GetNodeBuildInfoRequest
  ): Future[node_api.GetNodeBuildInfoResponse] = {
    val methodName = "getNodeBuildInfo"

    measureRequestFuture(serviceName, methodName)(
      trace { _ =>
        Future
          .successful(
            node_api
              .GetNodeBuildInfoResponse()
              .withVersion(BuildInfo.version)
              .withScalaVersion(BuildInfo.scalaVersion)
              .withSbtVersion(BuildInfo.sbtVersion)
          )
      }
    )
  }

  /** * PUBLIC
    *
    * Retrieves the Network Protocol info.
    */
  override def getNodeNetworkProtocolInfo(
      request: GetNodeNetworkProtocolInfoRequest
  ): Future[GetNodeNetworkProtocolInfoResponse] = {
    val methodName = "getNodeNetworkProtocolInfo"

    measureRequestFuture(serviceName, methodName)(
      trace { traceId =>
        val query =
          for {
            currentProtocolVersion <- nodeService.getCurrentProtocolVersion
          } yield node_api
            .GetNodeNetworkProtocolInfoResponse()
            .withSupportedNetworkProtocolVersion(SUPPORTED_VERSION.toProto)
            .withCurrentNetworkProtocolVersion(currentProtocolVersion.toProto)

        query.run(traceId).unsafeToFuture()
      }
    )
  }
}

object NodeGrpcServiceImpl {

  val serviceName = "node-service"

  def countAndThrowNodeError(methodName: String, error: NodeError): Nothing =
    RequestMeasureUtil.countAndThrowNodeError(serviceName)(methodName, error.toStatus)

  private def countAndThrowGetDidDocumentError[I](
      methodName: String,
      didRequestStr: String,
      error: GettingDidError
  ): I = {
    val errStatus = error match {
      case GettingCanonicalPrismDidError(nodeError) =>
        nodeError.toStatus
      case UnsupportedDidFormat =>
        Status.INVALID_ARGUMENT.withDescription(s"Invalid DID: $didRequestStr")
    }
    RequestMeasureUtil.increaseErrorCounter(
      serviceName,
      methodName,
      errStatus.getCode.value()
    )
    throw errStatus.asRuntimeException()
  }
}
