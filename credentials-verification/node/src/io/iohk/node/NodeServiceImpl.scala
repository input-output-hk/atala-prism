package io.iohk.node

import java.security.PublicKey

import com.google.protobuf.ByteString
import io.grpc.Status
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.errors.NodeError
import io.iohk.node.models.KeyUsage.{AuthenticationKey, CommunicationKey, IssuingKey, MasterKey}
import io.iohk.node.operations._
import io.iohk.node.services.{DIDDataService, ObjectManagementService}
import io.iohk.prism.protos.{node_api, node_models}

import scala.concurrent.{ExecutionContext, Future}

class NodeServiceImpl(didDataService: DIDDataService, objectManagement: ObjectManagementService)(
    implicit ec: ExecutionContext
) extends node_api.NodeServiceGrpc.NodeService {
  override def getDidDocument(request: node_api.GetDidDocumentRequest): Future[node_api.GetDidDocumentResponse] = {

    didDataService.findByDID(request.did).value.flatMap {
      case Left(err: NodeError) => Future.failed(err.toStatus.asRuntimeException())
      case Right(didData) =>
        Future.successful(node_api.GetDidDocumentResponse(Some(toDIDData(didData))))
    }
  }

  override def createDID(request: node_api.CreateDIDRequest): Future[node_api.CreateDIDResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      parsedOperation <- errorEitherToFuture(CreateDIDOperation.parse(operation))
      _ <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.CreateDIDResponse(id = parsedOperation.id.suffix)
  }

  override def updateDID(request: node_api.UpdateDIDRequest): Future[node_api.UpdateDIDResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      _ <- errorEitherToFuture(UpdateDIDOperation.parse(operation))
      _ <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.UpdateDIDResponse()
  }

  override def issueCredential(request: node_api.IssuerCredentialRequest): Future[node_api.IssueCredentialResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      parsedOperation <- errorEitherToFuture(IssueCredentialOperation.parse(operation))
      operation = request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
      _ <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.IssueCredentialResponse(id = parsedOperation.credentialId.id)
  }

  override def revokeCredential(
      request: node_api.RevokeCredentialRequest
  ): Future[node_api.RevokeCredentialResponse] = {
    val operationF = Future {
      request.signedOperation.getOrElse(throw new RuntimeException("signed_operation missing"))
    }
    for {
      operation <- operationF
      _ <- errorEitherToFuture(RevokeCredentialOperation.parse(operation))
      _ <- objectManagement.publishAtalaOperation(operation)
    } yield node_api.RevokeCredentialResponse()
  }

  private def toDIDData(didData: models.DIDData) = {
    node_models
      .DIDData()
      .withId(didData.didSuffix.suffix)
      .withPublicKeys(
        didData.keys.map(key => toProtoPublicKey(key.keyId, toECKeyData(key.key), toProtoKeyUsage(key.keyUsage)))
      )
  }

  private def toProtoPublicKey(
      id: String,
      ecKeyData: node_models.ECKeyData,
      keyUsage: node_models.KeyUsage
  ): node_models.PublicKey = {
    node_models
      .PublicKey()
      .withId(id)
      .withEcKeyData(ecKeyData)
      .withUsage(keyUsage)

  }

  private def toECKeyData(key: PublicKey): node_models.ECKeyData = {
    val point = ECKeys.getECPoint(key)
    node_models
      .ECKeyData()
      .withCurve(ECKeys.CURVE_NAME)
      .withX(ByteString.copyFrom(point.getAffineX.toByteArray))
      .withY(ByteString.copyFrom(point.getAffineY.toByteArray))
  }

  private def toProtoKeyUsage(keyUsage: models.KeyUsage): node_models.KeyUsage = {
    keyUsage match {
      case MasterKey => node_models.KeyUsage.MASTER_KEY
      case IssuingKey => node_models.KeyUsage.ISSUING_KEY
      case CommunicationKey => node_models.KeyUsage.COMMUNICATION_KEY
      case AuthenticationKey => node_models.KeyUsage.AUTHENTICATION_KEY

    }
  }

  protected def errorEitherToFuture[T](either: Either[ValidationError, T]): Future[T] = {
    Future.fromTry {
      either.left.map { error =>
        Status.INVALID_ARGUMENT.withDescription(error.render).asRuntimeException()
      }.toTry
    }
  }
}
