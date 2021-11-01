package io.iohk.atala.prism.node

import cats.effect.unsafe.IORuntime
import cats.syntax.applicative._
import com.google.protobuf.ByteString
import io.grpc.Status
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.metrics.RequestMeasureUtil
import io.iohk.atala.prism.metrics.RequestMeasureUtil.{FutureMetricsOps, measureRequestFuture}
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.logging.NodeLogging.{logWithTraceId, withLog}
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.models.{
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationInfo,
  AtalaOperationStatus
}
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.repositories.{CredentialBatchesRepository, DIDDataRepository}
import io.iohk.atala.prism.node.services.{ObjectManagementService, SubmissionSchedulingService}
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models.{OperationOutput, SignedAtalaOperation}
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import io.iohk.atala.prism.interop.toScalaProtos._
import io.iohk.atala.prism.crypto.{Sha256Digest => SHA256Digest}
import io.iohk.atala.prism.identity.{CanonicalPrismDid, LongFormPrismDid, PrismDid}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.cardano.models.AtalaObjectMetadata
import io.iohk.atala.prism.node.models.AtalaObjectTransactionSubmissionStatus.InLedger

class NodeServiceImpl(
    didDataRepository: DIDDataRepository[IOWithTraceIdContext],
    objectManagement: ObjectManagementService[IOWithTraceIdContext],
    submissionSchedulingService: SubmissionSchedulingService,
    credentialBatchesRepository: CredentialBatchesRepository[
      IOWithTraceIdContext
    ]
)(implicit ec: ExecutionContext, runtime: IORuntime)
    extends node_api.NodeServiceGrpc.NodeService {

  import NodeServiceImpl._

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
      withLog(methodName, request) { _ =>
        for {
          lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
            .run(TraceId.generateYOLO)
            .unsafeToFuture()
          response <- getDidDocument(request.did, methodName, didDataRepository)
        } yield response.withLastSyncedBlockTimestamp(
          lastSyncedTimestamp.toProtoTimestamp
        )
      }
    }

  }

  private def getDidDocument(
      didRequestStr: String,
      methodName: String,
      didDataRepository: DIDDataRepository[IOWithTraceIdContext]
  ) = {
    val didTry = Try(PrismDid.fromString(didRequestStr))
    val tid = TraceId.generateYOLO
    didTry match {
      case Success(did) =>
        did match {
          case canon: CanonicalPrismDid =>
            resolve(canon, tid, didDataRepository) orElse (countAndThrowNodeError(methodName, _))
          case longForm: LongFormPrismDid => // we received a long form DID
            // we check if the DID was published
            resolve(did.asCanonical(), did, tid, didDataRepository).orReturn {
              // if it was not published, we return the encoded initial state
              succeedWith(
                Some(
                  ProtoCodecs.atalaOperationToDIDDataProto(
                    DidSuffix(did.getSuffix),
                    longForm.getInitialState.asScala
                  )
                )
              )
            }
          case _ => failWith(s"Invalid DID: $didRequestStr", methodName)
        }
      case Failure(_) => failWith(s"Invalid DID: $didRequestStr", methodName)
    }
  }

  override def createDID(
      request: node_api.CreateDIDRequest
  ): Future[node_api.CreateDIDResponse] = {
    val methodName = "createDID"

    val operationF = getFromOptionOrFailF(
      request.signedOperation,
      "signed_operation missing",
      methodName
    )

    measureRequestFuture(serviceName, methodName) {
      withLog(methodName, request) { traceId =>
        for {
          operation <- operationF
          _ = logWithTraceId(
            methodName,
            traceId,
            "operationId" -> s"${AtalaOperationId.of(operation).toString}"
          )
          parsedOp <- errorEitherToFutureAndCount(
            methodName,
            CreateDIDOperation.parseWithMockedLedgerData(operation)
          )
          operationIdE <- objectManagement
            .scheduleSingleAtalaOperation(
              operation
            )
            .run(TraceId.generateYOLO)
            .unsafeToFuture()
        } yield {
          val response = node_api.CreateDIDResponse(id = parsedOp.id.getValue)
          operationIdE.fold(
            { err =>
              logger.warn(s"DID wasn't created, error: $err")
              response
            },
            operationId => response.withOperationId(operationId.toProtoByteString)
          )
        }
      }
    }
  }

  override def updateDID(
      request: node_api.UpdateDIDRequest
  ): Future[node_api.UpdateDIDResponse] = {
    val methodName = "updateDID"

    val operationF = getFromOptionOrFailF(
      request.signedOperation,
      "signed_operation missing",
      methodName
    )
    measureRequestFuture(serviceName, methodName) {
      withLog(methodName, request) { traceId =>
        for {
          operation <- operationF
          _ = logWithTraceId(
            methodName,
            traceId,
            "operationId" -> s"${AtalaOperationId.of(operation).toString}"
          )
          _ <- errorEitherToFutureAndCount(
            methodName,
            UpdateDIDOperation.validate(operation)
          )
          operationIdE <- objectManagement
            .scheduleSingleAtalaOperation(
              operation
            )
            .run(TraceId.generateYOLO)
            .unsafeToFuture()
        } yield {
          val response = node_api.UpdateDIDResponse()
          operationIdE.fold(
            { err =>
              logger.warn(s"DID wasn't updated, error: $err")
              response
            },
            operationId => response.withOperationId(operationId.toProtoByteString)
          )
        }
      }
    }
  }

  override def issueCredentialBatch(
      request: IssueCredentialBatchRequest
  ): Future[IssueCredentialBatchResponse] = {
    val methodName = "issueCredentialBatch"

    val operationF = getFromOptionOrFailF(
      request.signedOperation,
      "signed_operation missing",
      methodName
    )
    measureRequestFuture(serviceName, methodName) {
      withLog(methodName, request) { traceId =>
        for {
          operation <- operationF
          _ = logWithTraceId(
            methodName,
            traceId,
            "operationId" -> s"${AtalaOperationId.of(operation).toString}"
          )
          parsedOp <-
            errorEitherToFutureAndCount(
              methodName,
              IssueCredentialBatchOperation.parseWithMockedLedgerData(operation)
            )
          operationIdE <- objectManagement
            .scheduleSingleAtalaOperation(
              operation
            )
            .run(TraceId.generateYOLO)
            .unsafeToFuture()
        } yield {
          val response = node_api.IssueCredentialBatchResponse(batchId = parsedOp.credentialBatchId.getId)
          operationIdE.fold(
            { err =>
              logger.warn(s"Credentials weren't issued, error: $err")
              response
            },
            operationId => response.withOperationId(operationId.toProtoByteString)
          )
        }
      }
    }
  }

  override def revokeCredentials(
      request: RevokeCredentialsRequest
  ): Future[RevokeCredentialsResponse] = {
    val methodName = "revokeCredentials"
    val operationF = getFromOptionOrFailF(
      request.signedOperation,
      "signed_operation missing",
      methodName
    )
    measureRequestFuture(serviceName, methodName) {
      withLog(methodName, request) { traceId =>
        for {
          operation <- operationF
          _ = logWithTraceId(
            methodName,
            traceId,
            "operationId" -> s"${AtalaOperationId.of(operation).toString}"
          )
          _ <- errorEitherToFutureAndCount(
            methodName,
            RevokeCredentialsOperation.validate(operation)
          )
          operationIdE <- objectManagement
            .scheduleSingleAtalaOperation(
              operation
            )
            .run(TraceId.generateYOLO)
            .unsafeToFuture()
        } yield {
          val response = node_api.RevokeCredentialsResponse()
          operationIdE.fold(
            { err =>
              logger.warn(s"Credentials weren't revoked, error: $err")
              response
            },
            operationId => response.withOperationId(operationId.toProtoByteString)
          )
        }
      }
    }
  }

  override def getBatchState(
      request: GetBatchStateRequest
  ): Future[GetBatchStateResponse] = {
    val methodName = "getBatchState"

    val lastSyncedTimestampF = objectManagement.getLastSyncedTimestamp
    val batchIdF = getFromOptionOrFailF(
      Option(CredentialBatchId.fromString(request.batchId)),
      s"Invalid batch id: ${request.batchId}",
      methodName
    )

    measureRequestFuture(serviceName, methodName) {
      withLog(methodName, request) { traceId =>
        for {
          lastSyncedTimestamp <- lastSyncedTimestampF
            .run(TraceId.generateYOLO)
            .unsafeToFuture()
          batchId <- batchIdF
          _ = logWithTraceId(
            methodName,
            traceId,
            "batchId" -> s"${batchId.getId}"
          )
          stateEither <-
            credentialBatchesRepository
              .getBatchState(batchId)
              .run(TraceId.generateYOLO)
              .unsafeToFuture()
        } yield stateEither.fold(
          countAndThrowNodeError(methodName, _),
          toGetBatchResponse(_, lastSyncedTimestamp)
        )
      }

    }
  }

  override def getCredentialRevocationTime(
      request: GetCredentialRevocationTimeRequest
  ): Future[GetCredentialRevocationTimeResponse] = {
    val methodName = "getCredentialRevocationTime"

    val lastSyncedTimestampF = objectManagement.getLastSyncedTimestamp
    val batchIdF = getFromOptionOrFailF(
      Option(CredentialBatchId.fromString(request.batchId)),
      s"Invalid batch id: ${request.batchId}",
      methodName
    )

    val credentialHashF = Future
      .fromTry(Try(SHA256Digest.fromBytes(request.credentialHash.toByteArray)))
      .countErrorOnFail(
        serviceName,
        methodName,
        Status.INTERNAL.getCode.value()
      )
    measureRequestFuture(serviceName, methodName) {
      withLog(methodName, request) { traceId =>
        for {
          lastSyncedTimestamp <- lastSyncedTimestampF
            .run(TraceId.generateYOLO)
            .unsafeToFuture()
          batchId <- batchIdF
          _ = logWithTraceId(
            methodName,
            traceId,
            "batchId" -> s"${batchId.getId}"
          )
          credentialHash <- credentialHashF
          _ = logWithTraceId(
            methodName,
            traceId,
            "credentialHash" -> s"${credentialHash.getHexValue}"
          )
          timeEither <-
            credentialBatchesRepository
              .getCredentialRevocationTime(batchId, credentialHash)
              .run(TraceId.generateYOLO)
              .unsafeToFuture()
        } yield timeEither match {
          case Left(error) => countAndThrowNodeError(methodName, error)
          case Right(ledgerData) =>
            GetCredentialRevocationTimeResponse(
              revocationLedgerData = ledgerData.map(ProtoCodecs.toLedgerData)
            ).withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
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
            objectManagement
              .scheduleAtalaOperations(operations: _*)
              .run(TraceId.generateYOLO)
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

  override def flushOperationsBuffer(
      request: node_api.FlushOperationsBufferRequest
  ): Future[node_api.FlushOperationsBufferResponse] = {
    val methodName = "flushOperationsBuffer"

    withLog(methodName, request) { _ =>
      Future.successful {
        submissionSchedulingService.flushOperationsBuffer()
        node_api.FlushOperationsBufferResponse()
      }
    }
  }

  override def getOperationInfo(
      request: node_api.GetOperationInfoRequest
  ): Future[node_api.GetOperationInfoResponse] = {

    val methodName = "getOperationInfo"

    withLog(methodName, request) { traceId =>
      for {
        lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
        atalaOperationId = AtalaOperationId.fromVectorUnsafe(
          request.operationId.toByteArray.toVector
        )
        _ = logWithTraceId(
          methodName,
          traceId,
          "atalaOperationId" -> s"${atalaOperationId.toString}"
        )
        operationInfo <- objectManagement
          .getOperationInfo(atalaOperationId)
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
      } yield {
        val operationStatus = operationInfo
          .fold[common_models.OperationStatus](
            common_models.OperationStatus.UNKNOWN_OPERATION
          ) { case AtalaOperationInfo(_, _, opStatus, maybeTxStatus, _) =>
            evalOperationStatus(opStatus, maybeTxStatus)
          }
        val response = node_api
          .GetOperationInfoResponse()
          .withOperationStatus(operationStatus)
          .withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
        val responseWithTransactionId = operationInfo
          .flatMap(_.transactionId)
          .map(_.toString)
          .fold(response)(response.withTransactionId)

        responseWithTransactionId
      }
    }
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
      withLog(methodName, request) { _ =>
        objectManagement.getLastSyncedTimestamp
          .map { lastSyncedBlockTimestamp =>
            node_api
              .GetLastSyncedBlockTimestampResponse()
              .withLastSyncedBlockTimestamp(
                lastSyncedBlockTimestamp.toProtoTimestamp
              )
          }
          .run(TraceId.generateYOLO)
          .unsafeToFuture()
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

object NodeServiceImpl {

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
      in: Option[models.nodeState.CredentialBatchState],
      lastSyncedTimestamp: Instant
  ) = {
    val response = in.fold(GetBatchStateResponse()) { state =>
      val revocationLedgerData = state.revokedOn.map(ProtoCodecs.toLedgerData)
      val responseBase = GetBatchStateResponse()
        .withIssuerDid(state.issuerDIDSuffix.getValue)
        .withMerkleRoot(ByteString.copyFrom(state.merkleRoot.getHash.getValue))
        .withPublicationLedgerData(ProtoCodecs.toLedgerData(state.issuedOn))
      revocationLedgerData.fold(responseBase)(
        responseBase.withRevocationLedgerData
      )
    }
    response.withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
  }

  private def succeedWith(
      didData: Option[node_models.DIDData]
  )(implicit logger: Logger): Future[node_api.GetDidDocumentResponse] = {
    val response = node_api.GetDidDocumentResponse(document = didData)
    logger.info(s"response = ${response.toProtoString}")
    Future.successful(response)
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
    throw asStatus.asRuntimeException()
  }

  private def getFromOptionOrFailF[I](
      in: Option[I],
      msg: String,
      methodName: String
  )(implicit
      ec: ExecutionContext,
      logger: Logger
  ): Future[I] =
    in.fold { failWith[I](msg, methodName) }(_.pure[Future])

  private def failWith[I](msg: String, methodName: String)(implicit
      logger: Logger
  ): Future[I] = {
    logger.info(s"Failed with message: $msg")
    RequestMeasureUtil.increaseErrorCounter(
      serviceName,
      methodName,
      Status.INTERNAL.getCode.value()
    )
    Future.failed(new RuntimeException(msg))
  }

  private case class OrElse(
      did: DID,
      state: Future[Either[NodeError, Option[DIDDataState]]]
  ) {
    def orReturn(
        initialState: => Future[node_api.GetDidDocumentResponse]
    )(implicit
        ec: ExecutionContext,
        logger: Logger
    ): Future[node_api.GetDidDocumentResponse] =
      state.flatMap {
        case Right(stMaybe) =>
          stMaybe.fold(initialState)(st => succeedWith(Some(ProtoCodecs.toDIDDataProto(did.getSuffix, st))))
        case Left(err: NodeError) =>
          logger.info(err.toStatus.asRuntimeException().getMessage)
          initialState
      }

    def orElse(
        ifFailed: NodeError => Future[node_api.GetDidDocumentResponse]
    )(implicit
        ec: ExecutionContext,
        logger: Logger
    ): Future[node_api.GetDidDocumentResponse] =
      state.flatMap {
        case Right(stMaybe) =>
          val didData =
            stMaybe.map(st => ProtoCodecs.toDIDDataProto(did.getSuffix, st))
          succeedWith(didData)
        case Left(err: NodeError) => ifFailed(err)
      }
  }

  private def resolve(
      did: CanonicalPrismDid,
      butShowInDIDDocument: DID,
      tId: TraceId,
      didDataRepository: DIDDataRepository[IOWithTraceIdContext]
  )(implicit runtime: IORuntime): OrElse = {
    OrElse(
      butShowInDIDDocument,
      didDataRepository.findByDid(did).run(tId).unsafeToFuture()
    )
  }

  private def resolve(
      did: CanonicalPrismDid,
      tId: TraceId,
      didDataRepository: DIDDataRepository[IOWithTraceIdContext]
  )(implicit runtime: IORuntime): OrElse = {
    OrElse(did, didDataRepository.findByDid(did).run(tId).unsafeToFuture())
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
