package io.iohk.atala.prism.node

import io.grpc.Status
import io.iohk.atala.prism.BuildInfo
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.models.ProtoCodecs._
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.CredentialId
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.services.ObjectManagementService.AtalaObjectTransactionStatus
import io.iohk.atala.prism.node.services.{CredentialsService, DIDDataService, ObjectManagementService}
import io.iohk.atala.prism.protos.node_api.{
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

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class NodeServiceImpl(
    didDataService: DIDDataService,
    objectManagement: ObjectManagementService,
    credentialsService: CredentialsService
)(implicit
    ec: ExecutionContext
) extends node_api.NodeServiceGrpc.NodeService {

  import NodeServiceImpl._

  override def getDidDocument(request: node_api.GetDidDocumentRequest): Future[node_api.GetDidDocumentResponse] = {
    implicit val didService: DIDDataService = didDataService

    DID.getFormat(request.did) match {
      case DID.DIDFormat.Canonical(_) =>
        resolve(request.did) orElse (err => Future.failed(err.toStatus.asRuntimeException()))
      case longForm @ DID.DIDFormat.LongForm(stateHash, _) => // we received a long form DID
        // we first check that the encoded initial state matches the corresponding hash
        longForm.validate
          .map { validatedLongForm =>
            // validation succeeded, we check if the DID was published
            resolve(DID.buildPrismDID(stateHash), butShowInDIDDocument = request.did) orElse { _ =>
              // if it was not published, we return the encoded initial state
              succeedWith(
                ProtoCodecs.atalaOperationToDIDDataProto(
                  DID.stripPrismPrefix(request.did),
                  validatedLongForm.initialState
                )
              )
            }
          }
          .getOrElse(failWith(s"Invalid long form DID: ${request.did}"))
      case DID.DIDFormat.Unknown =>
        failWith(s"DID format not supported: ${request.did}")
    }
  }

  override def createDID(request: node_api.CreateDIDRequest): Future[node_api.CreateDIDResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      parsedOp <- errorEitherToFuture(CreateDIDOperation.parseWithMockedTime(operation))
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.CreateDIDResponse(id = parsedOp.id.suffix).withTransactionInfo(toTransactionInfo(transactionInfo))
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
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      parsedOp <- errorEitherToFuture(IssueCredentialOperation.parseWithMockedTime(operation))
      operation = request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield node_api
      .IssueCredentialResponse(id = parsedOp.credentialId.id)
      .withTransactionInfo(toTransactionInfo(transactionInfo))
  }

  override def revokeCredential(
      request: node_api.RevokeCredentialRequest
  ): Future[node_api.RevokeCredentialResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      _ <- errorEitherToFuture(RevokeCredentialOperation.validate(operation))
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.RevokeCredentialResponse().withTransactionInfo(toTransactionInfo(transactionInfo))
  }

  override def getCredentialState(request: GetCredentialStateRequest): Future[GetCredentialStateResponse] = {
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
        case Left(err: NodeError) => throw err.toStatus.asRuntimeException()
        case Right(credentialState) => ProtoCodecs.toCredentialStateResponseProto(credentialState)
      }
    }
  }

  override def getTransactionStatus(request: GetTransactionStatusRequest): Future[GetTransactionStatusResponse] = {
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
    } yield node_api
      .GetTransactionStatusResponse()
      .withTransactionInfo(toTransactionInfo(latestTransaction))
      .withStatus(status)
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
    Future
      .successful(
        node_api
          .GetNodeBuildInfoResponse()
          .withVersion(BuildInfo.version)
          .withScalaVersion(BuildInfo.scalaVersion)
          .withSbtVersion(BuildInfo.sbtVersion)
          .withBuildTime(BuildInfo.buildTime)
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
    val operationF = Future.fromTry {
      Try {
        request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
      }
    }

    for {
      operation <- operationF
      parsedOp <- errorEitherToFuture(IssueCredentialBatchOperation.parseWithMockedTime(operation))
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield node_api
      .IssueCredentialBatchResponse(batchId = parsedOp.credentialBatchId.id)
      .withTransactionInfo(toTransactionInfo(transactionInfo))
  }

  override def revokeCredentials(request: RevokeCredentialsRequest): Future[RevokeCredentialsResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      _ <- errorEitherToFuture(RevokeCredentialsOperation.validate(operation))
      transactionInfo <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.RevokeCredentialsResponse().withTransactionInfo(toTransactionInfo(transactionInfo))
  }
}

object NodeServiceImpl {
  private def succeedWith(didData: node_models.DIDData): Future[node_api.GetDidDocumentResponse] = {
    Future.successful(node_api.GetDidDocumentResponse(Some(didData)))
  }

  def failWith(msg: String): Future[node_api.GetDidDocumentResponse] = Future.failed(new RuntimeException(msg))

  private case class OrElse(did: String, state: Future[Either[NodeError, models.nodeState.DIDDataState]]) {
    def orElse(
        ifFailed: NodeError => Future[node_api.GetDidDocumentResponse]
    )(implicit ec: ExecutionContext): Future[node_api.GetDidDocumentResponse] =
      state.flatMap {
        case Right(st) =>
          succeedWith(ProtoCodecs.toDIDDataProto(DID.stripPrismPrefix(did), st))
        case Left(err: NodeError) => ifFailed(err)
      }
  }

  private def resolve(did: String, butShowInDIDDocument: String)(implicit didDataService: DIDDataService): OrElse = {
    OrElse(butShowInDIDDocument, didDataService.findByDID(did).value)
  }

  private def resolve(did: String)(implicit didDataService: DIDDataService): OrElse = {
    OrElse(did, didDataService.findByDID(did).value)
  }
}
