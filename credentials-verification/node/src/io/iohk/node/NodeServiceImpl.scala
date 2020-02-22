package io.iohk.node

import java.security.PublicKey

import com.google.protobuf.ByteString
import io.grpc.Status
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.errors.NodeError
import io.iohk.node.geud_node.NodeServiceGrpc.NodeService
import io.iohk.node.models.KeyUsage.{AuthenticationKey, CommunicationKey, IssuingKey, MasterKey}
import io.iohk.node.operations._
import io.iohk.node.services.{DIDDataService, ObjectManagementService}
import io.iohk.node.{geud_node => proto}

import scala.concurrent.{ExecutionContext, Future}

class NodeServiceImpl(didDataService: DIDDataService, objectManagement: ObjectManagementService)(
    implicit ec: ExecutionContext
) extends NodeService {
  override def getDidDocument(request: proto.GetDidDocumentRequest): Future[proto.GetDidDocumentResponse] = {

    didDataService.findByDID(request.did).value.flatMap {
      case Left(err: NodeError) => Future.failed(err.toStatus.asRuntimeException())
      case Right(didData) =>
        Future.successful(proto.GetDidDocumentResponse(Some(toDIDData(didData))))
    }
  }

  override def createDID(request: proto.SignedAtalaOperation): Future[proto.CreateDIDResponse] = {
    for {
      parsedOperation <- errorEitherToFuture(CreateDIDOperation.parse(request))
      _ <- objectManagement.publishAtalaOperation(request)
    } yield proto.CreateDIDResponse(id = parsedOperation.id.suffix)
  }

  override def updateDID(request: proto.SignedAtalaOperation): Future[proto.UpdateDIDResponse] = {
    for {
      _ <- errorEitherToFuture(UpdateDIDOperation.parse(request))
      _ <- objectManagement.publishAtalaOperation(request)
    } yield proto.UpdateDIDResponse()
  }

  override def issueCredential(request: proto.SignedAtalaOperation): Future[proto.IssueCredentialResponse] = {
    for {
      parsedOperation <- errorEitherToFuture(IssueCredentialOperation.parse(request))
      _ <- objectManagement.publishAtalaOperation(request)
    } yield proto.IssueCredentialResponse(id = parsedOperation.credentialId.id)
  }

  override def revokeCredential(request: proto.SignedAtalaOperation): Future[proto.RevokeCredentialResponse] = {
    for {
      _ <- errorEitherToFuture(RevokeCredentialOperation.parse(request))
      _ <- objectManagement.publishAtalaOperation(request)
    } yield proto.RevokeCredentialResponse()
  }

  private def toDIDData(didData: models.DIDData) = {
    proto
      .DIDData()
      .withId(didData.didSuffix.suffix)
      .withPublicKeys(
        didData.keys.map(key => toProtoPublicKey(key.keyId, toECKeyData(key.key), toProtoKeyUsage(key.keyUsage)))
      )
  }

  private def toProtoPublicKey(id: String, ecKeyData: proto.ECKeyData, keyUsage: proto.KeyUsage): proto.PublicKey = {
    proto
      .PublicKey()
      .withId(id)
      .withEcKeyData(ecKeyData)
      .withUsage(keyUsage)

  }

  private def toECKeyData(key: PublicKey): proto.ECKeyData = {
    val point = ECKeys.getECPoint(key)
    proto
      .ECKeyData()
      .withCurve(ECKeys.CURVE_NAME)
      .withX(ByteString.copyFrom(point.getAffineX.toByteArray))
      .withY(ByteString.copyFrom(point.getAffineY.toByteArray))
  }

  private def toProtoKeyUsage(keyUsage: models.KeyUsage): proto.KeyUsage = {
    keyUsage match {
      case MasterKey => proto.KeyUsage.MASTER_KEY
      case IssuingKey => proto.KeyUsage.ISSUING_KEY
      case CommunicationKey => proto.KeyUsage.COMMUNICATION_KEY
      case AuthenticationKey => proto.KeyUsage.AUTHENTICATION_KEY

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
