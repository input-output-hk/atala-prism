package io.iohk.atala.prism.node

import cats.syntax.applicative._
import com.google.protobuf.ByteString
import io.grpc.Status
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.metrics.RequestMeasureUtil
import io.iohk.atala.prism.metrics.RequestMeasureUtil.{FutureMetricsOps, measureRequestFuture}
import io.iohk.atala.prism.models.ProtoCodecs._
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.{
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationInfo,
  AtalaOperationStatus
}
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.repositories.{CredentialBatchesRepository, DIDDataRepository}
import io.iohk.atala.prism.node.services.ObjectManagementService.AtalaObjectTransactionStatus
import io.iohk.atala.prism.node.services.ObjectManagementService
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse, TransactionStatus}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models.AtalaOperation.Operation
import io.iohk.atala.prism.protos.node_models.{OperationOutput, SignedAtalaOperation}
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class NodeServiceImpl(
    didDataRepository: DIDDataRepository,
    objectManagement: ObjectManagementService,
    credentialBatchesRepository: CredentialBatchesRepository
)(implicit
    ec: ExecutionContext
) extends node_api.NodeServiceGrpc.NodeService {

  import NodeServiceImpl._

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(request: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse())

  override def getDidDocument(request: node_api.GetDidDocumentRequest): Future[node_api.GetDidDocumentResponse] = {
    val methodName = "getDidDocument"
    implicit val didDataRepositoryImplicit: DIDDataRepository = didDataRepository

    logRequest(methodName, request)
    measureRequestFuture(serviceName, methodName) {
      for {
        lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
        response <- getDidDocument(request.did, methodName)
      } yield response.withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
    }
  }

  private def getDidDocument(didRequestStr: String, methodName: String)(implicit
      didDataRepository: DIDDataRepository
  ) = {
    val didOpt = DID.fromString(didRequestStr)
    didOpt match {
      case Some(did) =>
        did.getFormat match {
          case DID.DIDFormat.Canonical(_) =>
            resolve(did) orElse (countAndThrowNodeError(methodName, _))
          case longForm @ DID.DIDFormat.LongForm(stateHash, _) => // we received a long form DID
            // we first check that the encoded initial state matches the corresponding hash
            longForm.validate
              .map { validatedLongForm =>
                // validation succeeded, we check if the DID was published
                resolve(DID.buildPrismDID(stateHash), did).orReturn {
                  // if it was not published, we return the encoded initial state
                  succeedWith(
                    Some(
                      ProtoCodecs.atalaOperationToDIDDataProto(
                        did.suffix,
                        validatedLongForm.initialState
                      )
                    )
                  )
                }
              }
              .getOrElse(failWith(s"Invalid long form DID: $didRequestStr", methodName))
          case DID.DIDFormat.Unknown =>
            failWith(s"DID format not supported: $didRequestStr", methodName)
        }
      case None =>
        failWith(s"Invalid DID: $didRequestStr", methodName)
    }
  }

  override def createDID(request: node_api.CreateDIDRequest): Future[node_api.CreateDIDResponse] = {
    val methodName = "createDID"
    logRequest(methodName, request)
    val operationF = getFromOptionOrFailF(request.signedOperation, "signed_operation missing", methodName)
    measureRequestFuture(serviceName, methodName) {
      for {
        operation <- operationF
        parsedOp <- errorEitherToFutureAndCount(methodName, CreateDIDOperation.parseWithMockedLedgerData(operation))
        operationId <- objectManagement.publishSingleAtalaOperation(operation)
      } yield {
        logAndReturnResponse(
          "createDID",
          node_api
            .CreateDIDResponse(id = parsedOp.id.value)
            .withOperationId(operationId.toProtoByteString)
        )
      }
    }
  }

  override def updateDID(request: node_api.UpdateDIDRequest): Future[node_api.UpdateDIDResponse] = {
    val methodName = "updateDID"
    val operationF = getFromOptionOrFailF(request.signedOperation, "signed_operation missing", methodName)
    measureRequestFuture(serviceName, methodName) {
      for {
        operation <- operationF
        _ <- errorEitherToFutureAndCount(methodName, UpdateDIDOperation.validate(operation))
        operationId <- objectManagement.publishSingleAtalaOperation(operation)
      } yield {
        logAndReturnResponse(
          "updateDID",
          node_api
            .UpdateDIDResponse()
            .withOperationId(operationId.toProtoByteString)
        )
      }
    }
  }

  override def issueCredentialBatch(request: IssueCredentialBatchRequest): Future[IssueCredentialBatchResponse] = {
    val methodName = "issueCredentialBatch"
    logRequest(methodName, request)
    val operationF = getFromOptionOrFailF(request.signedOperation, "signed_operation missing", methodName)
    measureRequestFuture(serviceName, methodName) {
      for {
        operation <- operationF
        parsedOp <-
          errorEitherToFutureAndCount(methodName, IssueCredentialBatchOperation.parseWithMockedLedgerData(operation))
        operationId <- objectManagement.publishSingleAtalaOperation(operation)
      } yield {
        logAndReturnResponse(
          methodName,
          node_api
            .IssueCredentialBatchResponse(batchId = parsedOp.credentialBatchId.id)
            .withOperationId(operationId.toProtoByteString)
        )
      }
    }
  }

  override def revokeCredentials(request: RevokeCredentialsRequest): Future[RevokeCredentialsResponse] = {
    val methodName = "revokeCredentials"
    logRequest(methodName, request)
    val operationF = getFromOptionOrFailF(request.signedOperation, "signed_operation missing", methodName)
    measureRequestFuture(serviceName, methodName) {
      for {
        operation <- operationF
        _ <- errorEitherToFutureAndCount(methodName, RevokeCredentialsOperation.validate(operation))
        operationId <- objectManagement.publishSingleAtalaOperation(operation)
      } yield {
        logAndReturnResponse(
          "revokeCredentials",
          node_api
            .RevokeCredentialsResponse()
            .withOperationId(operationId.toProtoByteString)
        )
      }
    }
  }

  override def getBatchState(request: GetBatchStateRequest): Future[GetBatchStateResponse] = {
    val methodName = "getBatchState"
    logRequest(methodName, request)
    val lastSyncedTimestampF = objectManagement.getLastSyncedTimestamp
    val batchIdF = getFromOptionOrFailF(
      CredentialBatchId.fromString(request.batchId),
      s"Invalid batch id: ${request.batchId}",
      methodName
    )

    measureRequestFuture(serviceName, methodName) {
      for {
        lastSyncedTimestamp <- lastSyncedTimestampF
        batchId <- batchIdF
        stateEither <-
          credentialBatchesRepository
            .getBatchState(batchId)
            .value
      } yield stateEither.fold(
        countAndThrowNodeError(methodName, _),
        toGetBatchResponse(_, lastSyncedTimestamp, methodName)
      )
    }
  }

  override def getCredentialRevocationTime(
      request: GetCredentialRevocationTimeRequest
  ): Future[GetCredentialRevocationTimeResponse] = {
    val methodName = "getCredentialRevocationTime"
    logRequest(methodName, request)
    val lastSyncedTimestampF = objectManagement.getLastSyncedTimestamp
    val batchIdF = getFromOptionOrFailF(
      CredentialBatchId.fromString(request.batchId),
      s"Invalid batch id: ${request.batchId}",
      methodName
    )

    val credentialHashF = Future
      .fromTry(Try(SHA256Digest.fromVectorUnsafe(request.credentialHash.toByteArray.toVector)))
      .countErrorOnFail(serviceName, methodName, Status.INTERNAL.getCode.value())
    measureRequestFuture(serviceName, methodName) {
      for {
        lastSyncedTimestamp <- lastSyncedTimestampF
        batchId <- batchIdF
        credentialHash <- credentialHashF
        timeEither <-
          credentialBatchesRepository
            .getCredentialRevocationTime(batchId, credentialHash)
            .value
      } yield timeEither match {
        case Left(error) => countAndThrowNodeError(methodName, error)
        case Right(ledgerData) =>
          logAndReturnResponse(
            methodName,
            GetCredentialRevocationTimeResponse(
              revocationLedgerData = ledgerData.map(ProtoCodecs.toLedgerData)
            ).withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
          )
      }
    }
  }

  override def publishAsABlock(request: PublishAsABlockRequest): Future[PublishAsABlockResponse] = {
    val methodName = "publishAsABlock"
    logRequest(methodName, request)
    val operationsF = Future
      .fromTry {
        Try {
          require(request.signedOperations.nonEmpty, "there must be at least one operation to be published")
          request.signedOperations
        }
      }
      .countErrorOnFail(serviceName, methodName, Status.INTERNAL.getCode.value())

    measureRequestFuture(serviceName, methodName) {
      for {
        operations <- operationsF
        outputs <- Future.sequence(
          operations.map { op =>
            errorEitherToFutureAndCount(methodName, parseOperationWithMockData(op))
          }
        )
        operationIds <- objectManagement.publishAtalaOperations(operations: _*)
        outputsWithOperationIds = outputs.zip(operationIds).map {
          case (out, opId) =>
            out.withOperationId(opId.toProtoByteString)
        }
      } yield {
        logAndReturnResponse(
          methodName,
          node_api
            .PublishAsABlockResponse()
            .withOutputs(outputsWithOperationIds)
        )
      }
    }
  }

  override def getTransactionStatus(request: GetTransactionStatusRequest): Future[GetTransactionStatusResponse] = {
    val methodName = "getTransactionStatus"
    def toTransactionStatus(status: AtalaObjectTransactionStatus): common_models.TransactionStatus = {
      status match {
        case AtalaObjectTransactionStatus.InLedger => common_models.TransactionStatus.IN_LEDGER
        case AtalaObjectTransactionStatus.Pending => common_models.TransactionStatus.PENDING
        case AtalaObjectTransactionStatus.Confirmed => common_models.TransactionStatus.CONFIRMED
      }
    }

    logRequest(methodName, request)
    val lastSyncedTimestampF = objectManagement.getLastSyncedTimestamp

    val transactionF =
      getFromOptionOrFailF(request.transactionInfo.map(fromTransactionInfo), "transaction_info is missing", methodName)
    measureRequestFuture(serviceName, methodName) {
      for {
        lastSyncedTimestamp <- lastSyncedTimestampF
        transaction <- transactionF
        latestTransactionAndStatus <- objectManagement.getLatestTransactionAndStatus(transaction)
        latestTransaction = latestTransactionAndStatus.map(_.transaction).getOrElse(transaction)
        status = latestTransactionAndStatus.fold[TransactionStatus](common_models.TransactionStatus.UNKNOWN)(info =>
          toTransactionStatus(info.status)
        )
      } yield {
        logAndReturnResponse(
          methodName,
          node_api
            .GetTransactionStatusResponse()
            .withTransactionInfo(toTransactionInfo(latestTransaction))
            .withStatus(status)
            .withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
        )
      }
    }
  }

  override def getOperationStatus(
      request: node_api.GetOperationStatusRequest
  ): Future[node_api.GetOperationStatusResponse] = {
    logRequest("getOperationStatus", request)
    for {
      lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
      atalaOperationId = AtalaOperationId.fromVectorUnsafe(request.operationId.toByteArray.toVector)
      operationInfo <- objectManagement.getOperationInfo(atalaOperationId)
    } yield {
      val operationStatus = operationInfo
        .fold[common_models.OperationStatus](common_models.OperationStatus.UNKNOWN_OPERATION) {
          case AtalaOperationInfo(_, _, opStatus, maybeTxStatus) =>
            evalOperationStatus(opStatus, maybeTxStatus)
        }
      logAndReturnResponse(
        "getOperationStatus",
        node_api
          .GetOperationStatusResponse()
          .withOperationStatus(operationStatus)
          .withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
      )
    }
  }

  private def evalOperationStatus(
      opStatus: AtalaOperationStatus,
      maybeTxStatus: Option[AtalaObjectTransactionSubmissionStatus]
  ): common_models.OperationStatus = {
    (opStatus, maybeTxStatus) match {
      case (AtalaOperationStatus.RECEIVED, None) =>
        common_models.OperationStatus.PENDING_SUBMISSION
      case (AtalaOperationStatus.RECEIVED, _) =>
        common_models.OperationStatus.AWAIT_CONFIRMATION
      case (AtalaOperationStatus.APPLIED, Some(AtalaObjectTransactionSubmissionStatus.InLedger)) =>
        common_models.OperationStatus.CONFIRMED_AND_APPLIED
      case (AtalaOperationStatus.REJECTED, Some(AtalaObjectTransactionSubmissionStatus.InLedger)) =>
        common_models.OperationStatus.CONFIRMED_AND_REJECTED
      case _ =>
        throw new RuntimeException(
          s"Unknown state of the operation: (operationStatus = $opStatus, transactionStatus = $maybeTxStatus)"
        )
    }
  }

  override def getNodeBuildInfo(
      request: node_api.GetNodeBuildInfoRequest
  ): Future[node_api.GetNodeBuildInfoResponse] = {
    val methodName = "getNodeBuildInfo"
    logRequest(methodName, request)
    measureRequestFuture(serviceName, methodName)(
      Future
        .successful(
          logAndReturnResponse(
            methodName,
            node_api
              .GetNodeBuildInfoResponse()
              .withVersion(BuildInfo.version)
              .withScalaVersion(BuildInfo.scalaVersion)
              .withSbtVersion(BuildInfo.sbtVersion)
          )
        )
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
      RequestMeasureUtil.increaseErrorCounter(serviceName, methodName, statusError.getCode.value())
      statusError.asRuntimeException()
    }.toFuture

  private def toGetBatchResponse(
      in: Option[models.nodeState.CredentialBatchState],
      lastSyncedTimestamp: Instant,
      methodName: String
  )(implicit logger: Logger) = {
    val response = in.fold(GetBatchStateResponse()) { state =>
      val revocationLedgerData = state.revokedOn.map(ProtoCodecs.toLedgerData)
      val responseBase = GetBatchStateResponse()
        .withIssuerDid(state.issuerDIDSuffix.value)
        .withMerkleRoot(ByteString.copyFrom(state.merkleRoot.hash.value.toArray))
        .withPublicationLedgerData(ProtoCodecs.toLedgerData(state.issuedOn))
      revocationLedgerData.fold(responseBase)(responseBase.withRevocationLedgerData)
    }
    logAndReturnResponse(
      methodName,
      response.withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
    )
  }

  private def succeedWith(
      didData: Option[node_models.DIDData]
  )(implicit logger: Logger): Future[node_api.GetDidDocumentResponse] = {
    val response = node_api.GetDidDocumentResponse(document = didData)
    logger.info(s"response = ${response.toProtoString}")
    Future.successful(response)
  }
// We need to rewrite the node service
  private def countAndThrowNodeError(methodName: String, error: NodeError): Nothing = {
    val asStatus = error.toStatus
    RequestMeasureUtil.increaseErrorCounter(serviceName, methodName, asStatus.getCode.value())
    throw asStatus.asRuntimeException()
  }

  private def getFromOptionOrFailF[I](in: Option[I], msg: String, methodName: String)(implicit
      ec: ExecutionContext,
      logger: Logger
  ): Future[I] =
    in.fold { failWith[I](msg, methodName) }(_.pure[Future])

  private def failWith[I](msg: String, methodName: String)(implicit
      logger: Logger
  ): Future[I] = {
    logger.info(s"Failed with message: $msg")
    RequestMeasureUtil.increaseErrorCounter(serviceName, methodName, Status.INTERNAL.getCode.value())
    Future.failed(new RuntimeException(msg))
  }

  private case class OrElse(did: DID, state: Future[Either[NodeError, Option[DIDDataState]]]) {
    def orReturn(
        initialState: => Future[node_api.GetDidDocumentResponse]
    )(implicit ec: ExecutionContext, logger: Logger): Future[node_api.GetDidDocumentResponse] =
      state.flatMap {
        case Right(stMaybe) =>
          stMaybe.fold(initialState)(st => succeedWith(Some(ProtoCodecs.toDIDDataProto(did.suffix.value, st))))
        case Left(err: NodeError) =>
          logger.info(err.toStatus.asRuntimeException().getMessage)
          initialState
      }

    def orElse(
        ifFailed: NodeError => Future[node_api.GetDidDocumentResponse]
    )(implicit ec: ExecutionContext, logger: Logger): Future[node_api.GetDidDocumentResponse] =
      state.flatMap {
        case Right(stMaybe) =>
          val didData = stMaybe.map(st => ProtoCodecs.toDIDDataProto(did.suffix.value, st))
          succeedWith(didData)
        case Left(err: NodeError) => ifFailed(err)
      }
  }

  private def resolve(did: DID, butShowInDIDDocument: DID)(implicit
      didDataRepository: DIDDataRepository
  ): OrElse = {
    OrElse(butShowInDIDDocument, didDataRepository.findByDid(did).value)
  }

  private def resolve(did: DID)(implicit didDataRepository: DIDDataRepository): OrElse = {
    OrElse(did, didDataRepository.findByDid(did).value)
  }

  def logRequest[Req <: GeneratedMessage](method: String, request: Req)(implicit logger: Logger): Unit = {
    logger.info(s"$method request = ${request.toProtoString}")
  }

  def logAndReturnResponse[Response <: GeneratedMessage](method: String, response: Response)(implicit
      logger: Logger
  ): Response = {
    logger.info(s"$method response = ${response.toProtoString}")
    response
  }

  private def parseOperationWithMockData(operation: SignedAtalaOperation): Either[ValidationError, OperationOutput] = {
    val operationEither = operation.getOperation.operation match {
      case Operation.Empty => // should not happen
        throw new RuntimeException("Unexpected empty AtalaOperation")
      case Operation.CreateDid(_) =>
        CreateDIDOperation.parseWithMockedLedgerData(operation).map { parsedOp =>
          OperationOutput(
            OperationOutput.Result.CreateDidOutput(
              node_models.CreateDIDOutput(parsedOp.id.value)
            )
          )
        }
      case Operation.UpdateDid(_) =>
        UpdateDIDOperation
          .parseWithMockedLedgerData(operation)
          .map { _ =>
            OperationOutput(
              OperationOutput.Result.UpdateDidOutput(
                node_models.UpdateDIDOutput()
              )
            )
          }
      case Operation.IssueCredentialBatch(_) =>
        IssueCredentialBatchOperation.parseWithMockedLedgerData(operation).map { parsedOp =>
          OperationOutput(
            OperationOutput.Result.BatchOutput(
              node_models.IssueCredentialBatchOutput(parsedOp.credentialBatchId.id)
            )
          )
        }
      case Operation.RevokeCredentials(_) =>
        RevokeCredentialsOperation
          .parseWithMockedLedgerData(operation)
          .map { _ =>
            OperationOutput(
              OperationOutput.Result.RevokeCredentialsOutput(
                node_models.RevokeCredentialsOutput()
              )
            )
          }
    }

    operationEither
  }
}
