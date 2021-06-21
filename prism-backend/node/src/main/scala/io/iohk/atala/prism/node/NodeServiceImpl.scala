package io.iohk.atala.prism.node

import com.google.protobuf.ByteString
import io.grpc.Status
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
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
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.node_api.{
  GetBatchStateRequest,
  GetBatchStateResponse,
  GetCredentialRevocationTimeRequest,
  GetCredentialRevocationTimeResponse,
  GetTransactionStatusRequest,
  GetTransactionStatusResponse,
  IssueCredentialBatchRequest,
  IssueCredentialBatchResponse,
  PublishAsABlockRequest,
  PublishAsABlockResponse,
  RevokeCredentialsRequest,
  RevokeCredentialsResponse
}
import io.iohk.atala.prism.protos.node_models.AtalaOperation.Operation
import io.iohk.atala.prism.protos.node_models.{OperationOutput, SignedAtalaOperation}
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

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
    implicit val didDataRepositoryImplicit: DIDDataRepository = didDataRepository

    logRequest("getDidDocument", request)

    for {
      lastSyncedTimestamp <- objectManagement.getLastSyncedTimestamp
      response <- getDidDocument(request.did)
    } yield response.withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
  }

  private def getDidDocument(didRequestStr: String)(implicit didDataRepository: DIDDataRepository) = {
    val didOpt = DID.fromString(didRequestStr)
    didOpt match {
      case Some(did) =>
        did.getFormat match {
          case DID.DIDFormat.Canonical(_) =>
            resolve(did) orElse (err => Future.failed(err.toStatus.asRuntimeException()))
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
              .getOrElse(failWith(s"Invalid long form DID: $didRequestStr"))
          case DID.DIDFormat.Unknown =>
            failWith(s"DID format not supported: $didRequestStr")
        }
      case None =>
        failWith(s"Invalid DID: $didRequestStr")
    }
  }

  override def createDID(request: node_api.CreateDIDRequest): Future[node_api.CreateDIDResponse] = {
    logRequest(s"createDID", request)
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      parsedOp <- errorEitherToFuture(CreateDIDOperation.parseWithMockedLedgerData(operation))
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

  override def updateDID(request: node_api.UpdateDIDRequest): Future[node_api.UpdateDIDResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      _ <- errorEitherToFuture(UpdateDIDOperation.validate(operation))
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

  override def issueCredentialBatch(request: IssueCredentialBatchRequest): Future[IssueCredentialBatchResponse] = {
    logRequest("issueCredentialBatch", request)
    val operationF = Future.fromTry {
      Try {
        request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
      }
    }

    for {
      operation <- operationF
      parsedOp <- errorEitherToFuture(IssueCredentialBatchOperation.parseWithMockedLedgerData(operation))
      operationId <- objectManagement.publishSingleAtalaOperation(operation)
    } yield {
      logAndReturnResponse(
        "issueCredentialBatch",
        node_api
          .IssueCredentialBatchResponse(batchId = parsedOp.credentialBatchId.id)
          .withOperationId(operationId.toProtoByteString)
      )
    }
  }

  override def revokeCredentials(request: RevokeCredentialsRequest): Future[RevokeCredentialsResponse] = {
    logRequest("revokeCredentials", request)
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      _ <- errorEitherToFuture(RevokeCredentialsOperation.validate(operation))
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

  override def getBatchState(request: GetBatchStateRequest): Future[GetBatchStateResponse] = {
    logRequest("getBatchState", request)
    val lastSyncedTimestampF = objectManagement.getLastSyncedTimestamp
    val batchIdF = Future.fromTry {
      Try {
        CredentialBatchId
          .fromString(request.batchId)
          .getOrElse(throw new RuntimeException(s"Invalid batch id: ${request.batchId}"))
      }
    }

    for {
      lastSyncedTimestamp <- lastSyncedTimestampF
      batchId <- batchIdF
      stateEither <-
        credentialBatchesRepository
          .getBatchState(batchId)
          .value
    } yield stateEither match {
      case Left(error) =>
        throw error.toStatus.asRuntimeException()
      case Right(maybeState) =>
        val response = maybeState.fold(GetBatchStateResponse()) { state =>
          val revocationLedgerData = state.revokedOn.map(ProtoCodecs.toLedgerData)
          val responseBase = GetBatchStateResponse()
            .withIssuerDid(state.issuerDIDSuffix.value)
            .withMerkleRoot(ByteString.copyFrom(state.merkleRoot.hash.value.toArray))
            .withPublicationLedgerData(ProtoCodecs.toLedgerData(state.issuedOn))
          revocationLedgerData.fold(responseBase)(responseBase.withRevocationLedgerData)
        }
        logAndReturnResponse(
          "getBatchState",
          response.withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
        )
    }
  }

  override def getCredentialRevocationTime(
      request: GetCredentialRevocationTimeRequest
  ): Future[GetCredentialRevocationTimeResponse] = {
    logRequest("getCredentialRevocationTime", request)
    val lastSyncedTimestampF = objectManagement.getLastSyncedTimestamp
    val batchIdF = Future.fromTry {
      Try {
        CredentialBatchId
          .fromString(request.batchId)
          .getOrElse(throw new RuntimeException(s"Invalid batch id: ${request.batchId}"))
      }
    }
    val credentialHashF = Future.fromTry {
      Try {
        SHA256Digest.fromVectorUnsafe(request.credentialHash.toByteArray.toVector)
      }
    }

    for {
      lastSyncedTimestamp <- lastSyncedTimestampF
      batchId <- batchIdF
      credentialHash <- credentialHashF
      timeEither <-
        credentialBatchesRepository
          .getCredentialRevocationTime(batchId, credentialHash)
          .value
    } yield timeEither match {
      case Left(error) =>
        throw error.toStatus.asRuntimeException()
      case Right(ledgerData) =>
        logAndReturnResponse(
          "getCredentialRevocationTime",
          GetCredentialRevocationTimeResponse(
            revocationLedgerData = ledgerData.map(ProtoCodecs.toLedgerData)
          ).withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
        )
    }
  }

  override def publishAsABlock(request: PublishAsABlockRequest): Future[PublishAsABlockResponse] = {
    logRequest("publishAsABlock", request)
    val operationsF = Future.fromTry {
      Try {
        require(request.signedOperations.nonEmpty, "there must be at least one operation to be published")
        request.signedOperations
      }
    }

    for {
      operations <- operationsF
      outputs <- Future.sequence(
        operations.map { op =>
          errorEitherToFuture(parseOperationWithMockData(op))
        }
      )
      operationIds <- objectManagement.publishAtalaOperations(operations: _*)
      outputsWithOperationIds = outputs.zip(operationIds).map {
        case (out, opId) =>
          out.withOperationId(opId.toProtoByteString)
      }
    } yield {
      logAndReturnResponse(
        "publishAsABlock",
        node_api
          .PublishAsABlockResponse()
          .withOutputs(outputsWithOperationIds)
      )
    }
  }

  override def getTransactionStatus(request: GetTransactionStatusRequest): Future[GetTransactionStatusResponse] = {
    def toTransactionStatus(status: AtalaObjectTransactionStatus): common_models.TransactionStatus = {
      status match {
        case AtalaObjectTransactionStatus.InLedger => common_models.TransactionStatus.IN_LEDGER
        case AtalaObjectTransactionStatus.Pending => common_models.TransactionStatus.PENDING
        case AtalaObjectTransactionStatus.Confirmed => common_models.TransactionStatus.CONFIRMED
      }
    }

    logRequest("getTransactionStatus", request)
    val lastSyncedTimestampF = objectManagement.getLastSyncedTimestamp

    val transactionF = Future {
      request.transactionInfo
        .map(fromTransactionInfo)
        .getOrElse(throw new RuntimeException("transaction_info is missing"))
    }

    for {
      lastSyncedTimestamp <- lastSyncedTimestampF
      transaction <- transactionF
      latestTransactionAndStatus <- objectManagement.getLatestTransactionAndStatus(transaction)
      latestTransaction = latestTransactionAndStatus.map(_.transaction).getOrElse(transaction)
      status =
        latestTransactionAndStatus
          .map(_.status)
          .map(toTransactionStatus)
          .getOrElse(common_models.TransactionStatus.UNKNOWN)
    } yield {
      logAndReturnResponse(
        "getTransactionStatus",
        node_api
          .GetTransactionStatusResponse()
          .withTransactionInfo(toTransactionInfo(latestTransaction))
          .withStatus(status)
          .withLastSyncedBlockTimestamp(lastSyncedTimestamp.toProtoTimestamp)
      )
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
    logRequest("getNodeBuildInfo", request)
    Future
      .successful(
        logAndReturnResponse(
          "getNodeBuildInfo",
          node_api
            .GetNodeBuildInfoResponse()
            .withVersion(BuildInfo.version)
            .withScalaVersion(BuildInfo.scalaVersion)
            .withSbtVersion(BuildInfo.sbtVersion)
        )
      )
  }

}

object NodeServiceImpl {
  def errorEitherToFuture[T](either: Either[ValidationError, T]): Future[T] = {
    Future.fromTry {
      either.left.map { error =>
        Status.INVALID_ARGUMENT.withDescription(error.render).asRuntimeException()
      }.toTry
    }
  }

  private def succeedWith(
      didData: Option[node_models.DIDData]
  )(implicit logger: Logger): Future[node_api.GetDidDocumentResponse] = {
    val response = node_api.GetDidDocumentResponse(document = didData)
    logger.info(s"response = ${response.toProtoString}")
    Future.successful(response)
  }

  def failWith(msg: String)(implicit logger: Logger): Future[node_api.GetDidDocumentResponse] = {
    logger.info(s"Failed with message: $msg")
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
