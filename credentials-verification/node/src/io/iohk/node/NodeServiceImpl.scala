package io.iohk.node

import io.grpc.Status
import io.iohk.cvp.BuildInfo
import io.iohk.node.errors.NodeError
import io.iohk.node.grpc.ProtoCodecs
import io.iohk.node.models.CredentialId
import io.iohk.node.operations._
import io.iohk.node.services.{CredentialsService, DIDDataService, ObjectManagementService}
import io.iohk.prism.protos.node_api.{GetCredentialStateRequest, GetCredentialStateResponse}
import io.iohk.prism.protos.node_api

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
      case Right(didData) =>
        Future.successful(node_api.GetDidDocumentResponse(Some(ProtoCodecs.toDIDDataProto(didData.toDIDData))))
    }
  }

  override def createDID(request: node_api.CreateDIDRequest): Future[node_api.CreateDIDResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      parsedOp <- errorEitherToFuture(CreateDIDOperation.parseWithMockedTime(operation))
      _ <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.CreateDIDResponse(id = parsedOp.id.suffix)
  }

  override def updateDID(request: node_api.UpdateDIDRequest): Future[node_api.UpdateDIDResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      _ <- errorEitherToFuture(UpdateDIDOperation.validate(operation))
      _ <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.UpdateDIDResponse()
  }

  override def issueCredential(request: node_api.IssuerCredentialRequest): Future[node_api.IssueCredentialResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      parsedOp <- errorEitherToFuture(IssueCredentialOperation.parseWithMockedTime(operation))
      operation = request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
      _ <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.IssueCredentialResponse(id = parsedOp.credentialId.id)
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
      _ <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.RevokeCredentialResponse()
  }

  override def getCredentialState(request: GetCredentialStateRequest): Future[GetCredentialStateResponse] = {
    val credentialIdF = Future {
      CredentialId(request.credentialId)
    } recover {
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

  override def getBuildInfo(request: node_api.GetBuildInfoRequest): Future[node_api.GetBuildInfoResponse] = {
    Future
      .successful(
        node_api
          .GetBuildInfoResponse()
          .withVersion(BuildInfo.version)
          .withScalaVersion(BuildInfo.scalaVersion)
          .withMillVersion(BuildInfo.millVersion)
          .withBuildTime(BuildInfo.buildTime)
      )
  }

  protected def errorEitherToFuture[T](either: Either[ValidationError, T]): Future[T] = {
    Future.fromTry {
      either.left.map { error =>
        Status.INVALID_ARGUMENT.withDescription(error.render).asRuntimeException()
      }.toTry
    }
  }
}
