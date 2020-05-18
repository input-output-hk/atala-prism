package io.iohk.cvp.cmanager.grpc.services

import java.util.UUID

import io.iohk.connector.Authenticator
import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.cvp.cmanager.models.requests.{CreateStudent, CreateSubject}
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Student}
import io.iohk.cvp.cmanager.repositories.{CredentialsRepository, IssuerSubjectsRepository, StudentsRepository}
import io.iohk.prism.protos.cmanager_api
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SubjectsServiceImpl(
    subjectsRepository: IssuerSubjectsRepository,
    authenticator: Authenticator
)(implicit
    ec: ExecutionContext
) extends cmanager_api.SubjectsServiceGrpc.SubjectsService {

  override def createSubject(request: cmanager_api.CreateSubjectRequest): Future[cmanager_api.CreateSubjectResponse] = {
    def f(issuerId: Issuer.Id) = {
      Future {
        lazy val json = io.circe.parser.parse(request.jsonData).getOrElse(throw new RuntimeException("Invalid json"))
        val model = request
          .into[CreateSubject]
          .withFieldConst(_.issuerId, issuerId)
          .withFieldConst(_.data, json)
          .enableUnsafeOption
          .transform

        subjectsRepository
          .create(model)
          .map(subjectToProto)
          .map(cmanager_api.CreateSubjectResponse().withSubject)
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("createStudent", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }
  }
}
