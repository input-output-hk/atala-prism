package io.iohk.cvp.cmanager.grpc.services

import java.util.UUID

import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.models.{Credential, Issuer, Student}
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.repositories.{CredentialsRepository, IssuersRepository}
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsServiceImpl(issuersRepository: IssuersRepository, credentialsRepository: CredentialsRepository)(
    implicit ec: ExecutionContext
) extends protos.CredentialsServiceGrpc.CredentialsService {

  override def createCredential(request: protos.CreateCredentialRequest): Future[protos.CreateCredentialResponse] = {
    val userId = getIssuerId()
    val studentId = Student.Id(UUID.fromString(request.studentId))
    val model = request
      .into[CreateCredential]
      .withFieldConst(_.issuedBy, userId)
      .withFieldConst(_.studentId, studentId)
      .enableUnsafeOption
      .transform

    credentialsRepository
      .create(model)
      .map(credentialToProto)
      .map(protos.CreateCredentialResponse().withCredential)
      .value
      .map {
        case Right(x) => x
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }

  override def getCredentials(request: protos.GetCredentialsRequest): Future[protos.GetCredentialsResponse] = {
    val lastSeenCredential = Try(UUID.fromString(request.lastSeenCredentialId)).map(Credential.Id.apply).toOption
    val userId = getIssuerId()
    credentialsRepository
      .getBy(userId, request.limit, lastSeenCredential)
      .map { list =>
        protos.GetCredentialsResponse(list.map(credentialToProto))
      }
      .value
      .map {
        case Right(x) => x
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }

  override def register(request: protos.RegisterRequest): Future[protos.RegisterResponse] = {
    val creationData = IssuersRepository.IssuerCreationData(
      Issuer.Name(request.name),
      request.did,
      Option(request.logo).filter(!_.isEmpty).map(_.toByteArray.toVector)
    )
    issuersRepository
      .insert(creationData)
      .value
      .map {
        case Right(id) => protos.RegisterResponse(issuerId = id.value.toString)
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }
}
