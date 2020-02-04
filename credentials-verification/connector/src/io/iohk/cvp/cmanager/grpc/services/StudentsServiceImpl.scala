package io.iohk.cvp.cmanager.grpc.services

import java.util.UUID

import io.iohk.connector.Authenticator
import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Student}
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.protos._
import io.iohk.cvp.cmanager.repositories.{CredentialsRepository, StudentsRepository}
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class StudentsServiceImpl(
    studentsRepository: StudentsRepository,
    credentialsRepository: CredentialsRepository,
    authenticator: Authenticator
)(
    implicit ec: ExecutionContext
) extends protos.StudentsServiceGrpc.StudentsService {

  override def createStudent(request: protos.CreateStudentRequest): Future[protos.CreateStudentResponse] = {
    def f(issuerId: Issuer.Id) = {
      Future {
        val model = request
          .into[CreateStudent]
          .withFieldConst(_.issuer, issuerId)
          .enableUnsafeOption
          .transform

        studentsRepository
          .create(model)
          .map(studentToProto)
          .map(protos.CreateStudentResponse().withStudent)
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

  override def getStudents(request: GetStudentsRequest): Future[GetStudentsResponse] = {
    def f(issuerId: Issuer.Id) = {
      Future {
        val lastSeenStudent = Try(UUID.fromString(request.lastSeenStudentId)).map(Student.Id.apply).toOption
        val groupName = Option(request.groupName.trim).filter(_.nonEmpty).map(IssuerGroup.Name.apply)

        studentsRepository
          .getBy(issuerId, request.limit, lastSeenStudent, groupName)
          .map { list =>
            protos.GetStudentsResponse(list.map(studentToProto))
          }
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("getStudents", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }

  }

  override def getStudent(request: GetStudentRequest): Future[GetStudentResponse] = {
    def f(issuerId: Issuer.Id) = {
      Future {
        val studentId = Student.Id(UUID.fromString(request.studentId))
        studentsRepository
          .find(issuerId, studentId)
          .map { maybe =>
            protos.GetStudentResponse(maybe.map(studentToProto))
          }
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("getStudent", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }

  }

  override def getStudentCredentials(request: GetStudentCredentialsRequest): Future[GetStudentCredentialsResponse] = {
    def f(issuerId: Issuer.Id) = {
      Future {
        val studentId = Student.Id(UUID.fromString(request.studentId))
        credentialsRepository
          .getBy(issuerId, studentId)
          .map { list =>
            protos.GetStudentCredentialsResponse(list.map(credentialToProto))
          }
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("getStudentCredentials", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }
  }

  override def generateConnectionToken(
      request: GenerateConnectionTokenRequest
  ): Future[GenerateConnectionTokenResponse] = {
    def f(issuerId: Issuer.Id) = {
      Future {
        val studentId = Student.Id.apply(UUID.fromString(request.studentId))

        studentsRepository
          .generateToken(issuerId, studentId)
          .map(token => protos.GenerateConnectionTokenResponse(token.token))
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("generateConnectionToken", request) { participantId =>
      f(Issuer.Id(participantId.uuid))
    }
  }
}
