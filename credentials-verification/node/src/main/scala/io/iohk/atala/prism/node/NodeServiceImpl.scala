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
import io.iohk.atala.prism.node.repositories.CredentialBatchesRepository
import io.iohk.atala.prism.node.services.ObjectManagementService.AtalaObjectTransactionStatus
import io.iohk.atala.prism.node.services.{CredentialsService, DIDDataService, ObjectManagementService}
import io.iohk.atala.prism.protos.node_api.{
  GetBatchStateRequest,
  GetBatchStateResponse,
  GetCredentialRevocationTimeRequest,
  GetCredentialRevocationTimeResponse,
  GetCredentialStateRequest,
  GetCredentialStateResponse,
  GetTransactionStatusRequest,
  GetTransactionStatusResponse,
  IssueCredentialBatchRequest,
  IssueCredentialBatchResponse,
  RevokeCredentialsRequest,
  RevokeCredentialsResponse
}
import io.iohk.atala.prism.protos.{common_models, node_api, node_models}
import io.iohk.atala.prism.utils.syntax._
import org.slf4j.{Logger, LoggerFactory}
import scalapb.GeneratedMessage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class NodeServiceImpl(
    didDataService: DIDDataService,
    objectManagement: ObjectManagementService,
    credentialsService: CredentialsService,
    credentialBatchesRepository: CredentialBatchesRepository
)(implicit
    ec: ExecutionContext
) extends node_api.NodeServiceGrpc.NodeService {

  import NodeServiceImpl._

  implicit val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def getDidDocument(request: node_api.GetDidDocumentRequest): Future[node_api.GetDidDocumentResponse] = {
    implicit val didService: DIDDataService = didDataService

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
                      did.stripPrismPrefix,
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
      parsedOp <- errorEitherToFuture(CreateDIDOperation.parseWithMockedTime(operation))
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield {
      logAndReturnResponse(
        "createDID",
        node_api.CreateDIDResponse(id = parsedOp.id.suffix).withTransactionInfo(toTransactionInfo(transactionInfo))
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
      parsedOp <- errorEitherToFuture(IssueCredentialOperation.parseWithMockedTime(operation))
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
      credentialStateEither <- credentialsService.getCredentialState(credentialId).value
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
            .withBuildTime(BuildInfo.buildTime)
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
      parsedOp <- errorEitherToFuture(IssueCredentialBatchOperation.parseWithMockedTime(operation))
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
      case Right(state) =>
        val revocationDateProto = state.revokedOn.map(ProtoCodecs.toTimeStampInfoProto)
        val responseBase = GetBatchStateResponse()
          .withIssuerDID(state.issuerDIDSuffix.suffix)
          .withMerkleRoot(ByteString.copyFrom(state.merkleRoot.hash.value.toArray))
          .withPublicationDate(ProtoCodecs.toTimeStampInfoProto(state.issuedOn))
        val response = revocationDateProto.fold(responseBase)(responseBase.withRevocationDate)
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
      case Right(timestampInfo) =>
        logAndReturnResponse(
          "getCredentialRevocationTime",
          GetCredentialRevocationTimeResponse(
            revocationDate = timestampInfo.map(ProtoCodecs.toTimeStampInfoProto)
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
          succeedWith(ProtoCodecs.toDIDDataProto(did.stripPrismPrefix, st))
        case Left(err: NodeError) => ifFailed(err)
      }
  }

  private def resolve(did: DID, butShowInDIDDocument: DID)(implicit didDataService: DIDDataService): OrElse = {
    OrElse(butShowInDIDDocument, didDataService.findByDID(did).value)
  }

  private def resolve(did: DID)(implicit didDataService: DIDDataService): OrElse = {
    OrElse(did, didDataService.findByDID(did).value)
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
}
