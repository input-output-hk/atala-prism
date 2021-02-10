package io.iohk.atala.prism.node

import com.google.protobuf.ByteString
import io.grpc.Status
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ProtoCodecs._
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.CredentialId
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.repositories.{CredentialsRepository, CredentialBatchesRepository, DIDDataRepository}
import io.iohk.atala.prism.node.services.ObjectManagementService.AtalaObjectTransactionStatus
import io.iohk.atala.prism.node.services.ObjectManagementService
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.node_api.{
  GetBatchStateRequest,
  GetBatchStateResponse,
  GetCredentialRevocationTimeRequest,
  GetCredentialRevocationTimeResponse,
  GetCredentialStateRequest,
  GetCredentialStateResponse,
  GetCredentialTransactionInfoRequest,
  GetCredentialTransactionInfoResponse,
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
    credentialsRepository: CredentialsRepository,
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

    val didOpt = DID.fromString(request.did)

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
                resolve(DID.buildPrismDID(stateHash), butShowInDIDDocument = did) orElse { _ =>
                  // if it was not published, we return the encoded initial state
                  succeedWith(
                    ProtoCodecs.atalaOperationToDIDDataProto(
                      did.suffix,
                      validatedLongForm.initialState
                    )
                  )
                }
              }
              .getOrElse(failWith(s"Invalid long form DID: ${request.did}"))
          case DID.DIDFormat.Unknown =>
            failWith(s"DID format not supported: ${request.did}")
        }
      case None =>
        failWith(s"Invalid DID: ${request.did}")
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
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield {
      logAndReturnResponse(
        "createDID",
        node_api.CreateDIDResponse(id = parsedOp.id.value).withTransactionInfo(toTransactionInfo(transactionInfo))
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
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.UpdateDIDResponse().withTransactionInfo(toTransactionInfo(transactionInfo))
  }

  override def issueCredential(request: node_api.IssueCredentialRequest): Future[node_api.IssueCredentialResponse] = {
    logRequest(s"issueCredential", request)
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      parsedOp <- errorEitherToFuture(IssueCredentialOperation.parseWithMockedLedgerData(operation))
      operation = request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield {
      logAndReturnResponse(
        "issueCredential",
        node_api
          .IssueCredentialResponse(id = parsedOp.credentialId.id)
          .withTransactionInfo(toTransactionInfo(transactionInfo))
      )
    }
  }

  override def revokeCredential(
      request: node_api.RevokeCredentialRequest
  ): Future[node_api.RevokeCredentialResponse] = {
    logRequest("revokeCredential", request)
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      _ <- errorEitherToFuture(RevokeCredentialOperation.validate(operation))
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield {
      logAndReturnResponse(
        "revokeCredential",
        node_api.RevokeCredentialResponse().withTransactionInfo(toTransactionInfo(transactionInfo))
      )
    }
  }

  override def getCredentialState(request: GetCredentialStateRequest): Future[GetCredentialStateResponse] = {
    logRequest("getCredentialState", request)
    val credentialIdF =
      CredentialId(request.credentialId).tryF recover {
        case ex: IllegalArgumentException =>
          throw new RuntimeException(ex.getMessage)
      }
    for {
      credentialId <- credentialIdF
      credentialStateEither <- credentialsRepository.getCredentialState(credentialId).value
    } yield {
      credentialStateEither match {
        case Left(err: NodeError) =>
          logger.info(s"Failed to retrieve state: $err")
          throw err.toStatus.asRuntimeException()
        case Right(credentialState) =>
          logAndReturnResponse(
            "getCredentialState",
            ProtoCodecs.toCredentialStateResponseProto(credentialState)
          )
      }
    }
  }

  override def getTransactionStatus(request: GetTransactionStatusRequest): Future[GetTransactionStatusResponse] = {
    logRequest("getTransactionStatus", request)
    val transactionF = Future {
      request.transactionInfo
        .map(fromTransactionInfo)
        .getOrElse(throw new RuntimeException("transaction_info is missing"))
    }

    for {
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
      )
    }
  }

  private def toTransactionStatus(status: AtalaObjectTransactionStatus): common_models.TransactionStatus = {
    status match {
      case AtalaObjectTransactionStatus.InLedger => common_models.TransactionStatus.IN_LEDGER
      case AtalaObjectTransactionStatus.Pending => common_models.TransactionStatus.PENDING
      case AtalaObjectTransactionStatus.Confirmed => common_models.TransactionStatus.CONFIRMED
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

  private def errorEitherToFuture[T](either: Either[ValidationError, T]): Future[T] = {
    Future.fromTry {
      either.left.map { error =>
        Status.INVALID_ARGUMENT.withDescription(error.render).asRuntimeException()
      }.toTry
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
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield {
      logAndReturnResponse(
        "issueCredentialBatch",
        node_api
          .IssueCredentialBatchResponse(batchId = parsedOp.credentialBatchId.id)
          .withTransactionInfo(toTransactionInfo(transactionInfo))
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
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield {
      logAndReturnResponse(
        "revokeCredentials",
        node_api.RevokeCredentialsResponse().withTransactionInfo(toTransactionInfo(transactionInfo))
      )
    }
  }

  override def getBatchState(request: GetBatchStateRequest): Future[GetBatchStateResponse] = {
    logRequest("getBatchState", request)
    val batchIdF = Future.fromTry {
      Try {
        CredentialBatchId
          .fromString(request.batchId)
          .getOrElse(throw new RuntimeException(s"Invalid batch id: ${request.batchId}"))
      }
    }

    for {
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
            .withIssuerDID(state.issuerDIDSuffix.value)
            .withMerkleRoot(ByteString.copyFrom(state.merkleRoot.hash.value.toArray))
            .withPublicationLedgerData(ProtoCodecs.toLedgerData(state.issuedOn))
          revocationLedgerData.fold(responseBase)(responseBase.withRevocationLedgerData)
        }
        logAndReturnResponse(
          "getBatchState",
          response
        )
    }
  }

  override def getCredentialRevocationTime(
      request: GetCredentialRevocationTimeRequest
  ): Future[GetCredentialRevocationTimeResponse] = {
    logRequest("getCredentialRevocationTime", request)
    val batchIdF = Future.fromTry {
      Try {
        CredentialBatchId
          .fromString(request.batchId)
          .getOrElse(throw new RuntimeException(s"Invalid batch id: ${request.batchId}"))
      }
    }
    val credentialHashF = Future.fromTry {
      Try {
        SHA256Digest(request.credentialHash.toByteArray.toVector)
      }
    }

    for {
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
          )
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
      transactionInfo <- objectManagement.publishAtalaOperation(operations: _*)
    } yield {
      logAndReturnResponse(
        "publishAsABlock",
        node_api
          .PublishAsABlockResponse()
          .withTransactionInfo(toTransactionInfo(transactionInfo))
          .withOutputs(outputs)
      )
    }
  }

  /** NOTE: This will be removed after migrating to slayer 0.3
    * Returns the transaction information associated to the credential (both issuance and possible revocation)
    */
  override def getCredentialTransactionInfo(
      request: GetCredentialTransactionInfoRequest
  ): Future[GetCredentialTransactionInfoResponse] = {
    logRequest("getCredentialTransactionInfo", request)
    val credentialIdF = Future.fromTry(
      Try { CredentialId(request.credentialId) }
    )
    for {
      credentialId <- credentialIdF
      transactionInfoEither <- credentialsRepository.getCredentialTransactionInfo(credentialId).value
    } yield transactionInfoEither match {
      case Left(error) =>
        throw error.toStatus.asRuntimeException()
      case Right(transactionInfo) =>
        logAndReturnResponse(
          "getCredentialTransactionInfo",
          node_api.GetCredentialTransactionInfoResponse(
            issuance = transactionInfo.map(toTransactionInfo)
          )
        )
    }
  }
}

object NodeServiceImpl {
  private def succeedWith(
      didData: node_models.DIDData
  )(implicit logger: Logger): Future[node_api.GetDidDocumentResponse] = {
    val response = node_api.GetDidDocumentResponse(Some(didData))
    logger.info(s"response = ${response.toProtoString}")
    Future.successful(response)
  }

  def failWith(msg: String)(implicit logger: Logger): Future[node_api.GetDidDocumentResponse] = {
    logger.info(s"Failed with message: $msg")
    Future.failed(new RuntimeException(msg))
  }

  private case class OrElse(did: DID, state: Future[Either[NodeError, models.nodeState.DIDDataState]]) {
    def orElse(
        ifFailed: NodeError => Future[node_api.GetDidDocumentResponse]
    )(implicit ec: ExecutionContext, logger: Logger): Future[node_api.GetDidDocumentResponse] =
      state.flatMap {
        case Right(st) =>
          succeedWith(ProtoCodecs.toDIDDataProto(did.suffix.value, st))
        case Left(err: NodeError) => ifFailed(err)
      }
  }

  private def resolve(did: DID, butShowInDIDDocument: DID)(implicit didDataRepository: DIDDataRepository): OrElse = {
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
    operation.getOperation.operation match {
      case Operation.Empty => // should not happen
        throw new RuntimeException("Unexpected empty AtalaOperation")
      case Operation.CreateDid(_) =>
        CreateDIDOperation.parseWithMockedLedgerData(operation).map { parsedOp =>
          OperationOutput(
            OperationOutput.Result.CreateDIDOutput(
              node_models.CreateDIDOutput(parsedOp.id.value)
            )
          )
        }
      case Operation.UpdateDid(_) =>
        UpdateDIDOperation
          .parseWithMockedLedgerData(operation)
          .map { _ =>
            OperationOutput(
              OperationOutput.Result.UpdateDIDOutput(
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
      case Operation.IssueCredential(_) =>
        // we are deprecating this one soon, so we leave a not implemented error
        throw new NotImplementedError("IssueCredential is deprecated and cannot be used in a block")
      case Operation.RevokeCredential(_) =>
        // we are deprecating this one soon, so we leave a not implemented error
        throw new NotImplementedError("RevokeCredential is deprecated and cannot be used in a block")
    }
  }
}
