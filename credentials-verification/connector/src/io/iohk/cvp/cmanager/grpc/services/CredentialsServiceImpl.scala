package io.iohk.cvp.cmanager.grpc.services

import java.util.UUID

import io.iohk.connector.Authenticator
import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.models.{Credential, Issuer, Student}
import io.iohk.cvp.cmanager.repositories.{CredentialsRepository, IssuersRepository}
import io.iohk.prism.protos.cmanager_api
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CredentialsServiceImpl(
    issuersRepository: IssuersRepository,
    credentialsRepository: CredentialsRepository,
    authenticator: Authenticator
)(implicit
    ec: ExecutionContext
) extends cmanager_api.CredentialsServiceGrpc.CredentialsService {

  override def createCredential(
      request: cmanager_api.CreateCredentialRequest
  ): Future[cmanager_api.CreateCredentialResponse] = {
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
          .map(cmanager_api.CreateCredentialResponse().withCredential)
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

  override def getCredentials(
      request: cmanager_api.GetCredentialsRequest
  ): Future[cmanager_api.GetCredentialsResponse] = {

    def f(issuerId: Issuer.Id) = {
      Future {
        val lastSeenCredential = Try(UUID.fromString(request.lastSeenCredentialId)).map(Credential.Id.apply).toOption
        credentialsRepository
          .getBy(issuerId, request.limit, lastSeenCredential)
          .map { list =>
            cmanager_api.GetCredentialsResponse(list.map(credentialToProto))
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
}
