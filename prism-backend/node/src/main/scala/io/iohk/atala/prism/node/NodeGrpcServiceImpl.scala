package io.iohk.atala.prism.node

import cats.effect.unsafe.IORuntime
import com.google.protobuf.ByteString
import io.grpc.Status
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.metrics.RequestMeasureUtil
import io.iohk.atala.prism.metrics.RequestMeasureUtil.{FutureMetricsOps, measureRequestFuture}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.logging.NodeLogging.{logWithTraceId, withLog}
import io.iohk.atala.prism.node.models.{
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationInfo,
  AtalaOperationStatus
}
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.services.{
  BatchData,
  GettingCanonicalPrismDidError,
  GettingDidError,
  NodeService,
  ObjectManagementService,
  UnsupportedDidFormat
}
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models.{OperationOutput, SignedAtalaOperation}
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}
import io.iohk.atala.prism.tracing.Tracing._
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.node.cardano.models.AtalaObjectMetadata
import io.iohk.atala.prism.node.models.AtalaObjectTransactionSubmissionStatus.InLedger

class NodeGrpcServiceImpl(nodeService: NodeService[IOWithTraceIdContext])(implicit
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

  override def getBatchState(
      request: GetBatchStateRequest
  ): Future[GetBatchStateResponse] = {
    val methodName = "getBatchState"

    measureRequestFuture(serviceName, methodName) {
      trace { traceId =>
        val query = for {
          batchState <- nodeService.getBatchState(request.batchId)
        } yield batchState.fold(
          countAndThrowNodeError(methodName, _),
          toGetBatchResponse
        )
        query.run(traceId).unsafeToFuture()
      }

    }
  }

  override def getCredentialRevocationTime(
      request: GetCredentialRevocationTimeRequest
  ): Future[GetCredentialRevocationTimeResponse] = {
    val methodName = "getCredentialRevocationTime"

    measureRequestFuture(serviceName, methodName) {
      trace { traceId =>
        for {
          revocationTimeEither <-
            nodeService
              .getCredentialRevocationData(request.batchId, request.credentialHash)
              .run(traceId)
              .unsafeToFuture()
        } yield revocationTimeEither match {
          case Left(error) => countAndThrowNodeError(methodName, error)
          case Right(ledgerData) =>
            GetCredentialRevocationTimeResponse(
              revocationLedgerData = ledgerData.maybeLedgerData.map(ProtoCodecs.toLedgerData)
            ).withLastSyncedBlockTimestamp(ledgerData.lastSyncedTimestamp.toProtoTimestamp)
        }
      }
    }
  }

  override def scheduleOperations(
      request: node_api.ScheduleOperationsRequest
  ): Future[node_api.ScheduleOperationsResponse] = {
    val methodName = "scheduleOperations"

    val operationsF = Future
      .fromTry {
        Try {
          require(
            request.signedOperations.nonEmpty,
            "there must be at least one operation to be published"
          )

          request.signedOperations.map { signedAtalaOperation =>
            val obj = ObjectManagementService.createAtalaObject(
              List(signedAtalaOperation)
            )
            val operationId = AtalaOperationId.of(signedAtalaOperation)
            require(
              AtalaObjectMetadata.estimateTxMetadataSize(
                obj
              ) <= cardano.TX_METADATA_MAX_SIZE,
              s"atala operation $operationId is too big"
            )
            signedAtalaOperation
          }
        }
      }
      .countErrorOnFail(
        serviceName,
        methodName,
        Status.INTERNAL.getCode.value()
      )

    measureRequestFuture(serviceName, methodName) {
      withLog(methodName, request) { traceId =>
        for {
          operations <- operationsF
          operationIds = operations.map(AtalaOperationId.of)
          _ = logWithTraceId(
            methodName,
            traceId,
            "operations" -> operationIds.map(_.toString).mkString(",")
          )
          outputs <- Future.sequence(
            operations.map { op =>
              errorEitherToFutureAndCount(methodName, getOperationOutput(op))
            }
          )
          operationIds <-
            nodeService
              .scheduleAtalaOperations(operations: _*)
              .run(traceId)
              .unsafeToFuture()
          outputsWithOperationIds = outputs.zip(operationIds).map {
            case (out, Right(opId)) =>
              out.withOperationId(opId.toProtoByteString)
            case (out, Left(err)) =>
              out.withError(err.toString)
          }
        } yield {
          node_api
            .ScheduleOperationsResponse()
            .withOutputs(outputsWithOperationIds)
        }
      }
    }
  }

  override def getOperationInfo(
      request: node_api.GetOperationInfoRequest
  ): Future[node_api.GetOperationInfoResponse] = {

    val methodName = "getOperationInfo"

    withLog(methodName, request) { traceId =>
      val atalaOperationId = AtalaOperationId.fromVectorUnsafe(
        request.operationId.toByteArray.toVector
      )
      logWithTraceId(
        methodName,
        traceId,
        "atalaOperationId" -> s"${atalaOperationId.toString}"
      )
      for {
        operationInfo <- nodeService
          .getOperationInfo(atalaOperationId)
          .run(traceId)
          .unsafeToFuture()
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
      withLog(methodName, request) { _ =>
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
}

object NodeGrpcServiceImpl {

  val serviceName = "node-service"

  private def errorEitherToFutureAndCount[T](
      methodName: String,
      either: Either[ValidationError, T]
  ): Future[T] =
    either.left.map { error =>
      val statusError = Status.INVALID_ARGUMENT.withDescription(error.render)
      RequestMeasureUtil.increaseErrorCounter(
        serviceName,
        methodName,
        statusError.getCode.value()
      )
      statusError.asRuntimeException()
    }.toFuture

  private def toGetBatchResponse(
      in: BatchData
  ) = {
    val response = in.maybeBatchState.fold(GetBatchStateResponse()) { state =>
      val revocationLedgerData = state.revokedOn.map(ProtoCodecs.toLedgerData)
      val responseBase = GetBatchStateResponse()
        .withIssuerDid(state.issuerDIDSuffix.getValue)
        .withMerkleRoot(ByteString.copyFrom(state.merkleRoot.getHash.getValue))
        .withPublicationLedgerData(ProtoCodecs.toLedgerData(state.issuedOn))
      revocationLedgerData.fold(responseBase)(
        responseBase.withRevocationLedgerData
      )
    }
    response.withLastSyncedBlockTimestamp(in.lastSyncedTimestamp.toProtoTimestamp)
  }

// We need to rewrite the node service
  private def countAndThrowNodeError(
      methodName: String,
      error: NodeError
  ): Nothing = {
    val asStatus = error.toStatus
    RequestMeasureUtil.increaseErrorCounter(
      serviceName,
      methodName,
      asStatus.getCode.value()
    )
    throw new RuntimeException(asStatus.getDescription)
  }

  private def countAndThrowGetDidDocumentError[I](
      methodName: String,
      didRequestStr: String,
      error: GettingDidError
  ): I = {
    val errStatus = error match {
      case GettingCanonicalPrismDidError(nodeError) =>
        nodeError.toStatus
      case UnsupportedDidFormat =>
        Status.INTERNAL.withDescription(s"Invalid DID: $didRequestStr")
    }
    RequestMeasureUtil.increaseErrorCounter(
      serviceName,
      methodName,
      errStatus.getCode.value()
    )
    throw errStatus.asRuntimeException()
  }

  private def getOperationOutput(
      operation: SignedAtalaOperation
  ): Either[ValidationError, OperationOutput] =
    parseOperationWithMockedLedger(operation).map {
      case CreateDIDOperation(id, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.CreateDidOutput(
            node_models.CreateDIDOutput(id.getValue)
          )
        )
      case UpdateDIDOperation(_, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.UpdateDidOutput(
            node_models.UpdateDIDOutput()
          )
        )
      case IssueCredentialBatchOperation(credentialBatchId, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.BatchOutput(
            node_models.IssueCredentialBatchOutput(credentialBatchId.getId)
          )
        )
      case RevokeCredentialsOperation(_, _, _, _, _) =>
        OperationOutput(
          OperationOutput.Result.RevokeCredentialsOutput(
            node_models.RevokeCredentialsOutput()
          )
        )
      case other =>
        throw new IllegalArgumentException(
          "Unknown operation type: " + other.getClass
        )
    }
}
