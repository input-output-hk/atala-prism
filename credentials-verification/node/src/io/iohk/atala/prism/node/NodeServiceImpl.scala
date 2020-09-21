package io.iohk.atala.prism.node

import io.grpc.Status
import io.iohk.atala.prism.models.ProtoCodecs._
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.models.CredentialId
import io.iohk.atala.prism.node.operations._
import io.iohk.atala.prism.node.services.{CredentialsService, DIDDataService, ObjectManagementService}
import io.iohk.atala.prism.utils.syntax._
import io.iohk.cvp.BuildInfo
import io.iohk.prism.protos.node_api
import io.iohk.prism.protos.node_api.{GetCredentialStateRequest, GetCredentialStateResponse}

import scala.concurrent.{ExecutionContext, Future}

class NodeServiceImpl(
    didDataService: DIDDataService,
    objectManagement: ObjectManagementService,
    credentialsService: CredentialsService
)(implicit
    ec: ExecutionContext
) extends node_api.NodeServiceGrpc.NodeService {
  override def getDidDocument(request: node_api.GetDidDocumentRequest): Future[node_api.GetDidDocumentResponse] = {

    didDataService.findByDID(request.did).value.flatMap {
      case Left(err: NodeError) => Future.failed(err.toStatus.asRuntimeException())
      case Right(didDataState) =>
        Future.successful(node_api.GetDidDocumentResponse(Some(ProtoCodecs.toDIDDataProto(didDataState))))
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

  override def getNodeBuildInfo(
      request: node_api.GetNodeBuildInfoRequest
  ): Future[node_api.GetNodeBuildInfoResponse] = {
    Future
      .successful(
        node_api
          .GetNodeBuildInfoResponse()
          .withVersion(BuildInfo.version)
          .withScalaVersion(BuildInfo.scalaVersion)
          .withMillVersion(BuildInfo.millVersion)
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
}
