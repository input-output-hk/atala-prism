package io.iohk.cvp.cmanager.grpc.services

import java.util.UUID

import io.iohk.connector.Authenticator
import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.models.{Credential, Issuer, Student}
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.repositories.{CredentialsRepository, IssuersRepository}
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsServiceImpl(
    issuersRepository: IssuersRepository,
    credentialsRepository: CredentialsRepository,
    authenticator: Authenticator
)(
    implicit ec: ExecutionContext
) extends protos.CredentialsServiceGrpc.CredentialsService {

  override def createCredential(request: protos.CreateCredentialRequest): Future[protos.CreateCredentialResponse] = {
    def f(issuerId: Issuer.Id) = {
      Future {
        val studentId = Student.Id(UUID.fromString(request.studentId))
        val model = request
          .into[CreateCredential]
          .withFieldConst(_.issuedBy, issuerId)
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
      }.flatten
    }

    authenticator.authenticated("createCredential", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }

  }

  override def getCredentials(request: protos.GetCredentialsRequest): Future[protos.GetCredentialsResponse] = {

    def f(issuerId: Issuer.Id) = {
      Future {
        val lastSeenCredential = Try(UUID.fromString(request.lastSeenCredentialId)).map(Credential.Id.apply).toOption
        credentialsRepository
          .getBy(issuerId, request.limit, lastSeenCredential)
          .map { list =>
            protos.GetCredentialsResponse(list.map(credentialToProto))
          }
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("getCredentials", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }

  }

  override def register(request: protos.RegisterRequest): Future[protos.RegisterResponse] = {
    authenticator.public("register", request) {
      Future {
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
      }.flatten
    }
  }

}
