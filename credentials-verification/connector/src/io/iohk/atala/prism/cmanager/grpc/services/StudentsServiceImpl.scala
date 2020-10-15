package io.iohk.atala.prism.cmanager.grpc.services

import java.util.UUID

import io.iohk.atala.prism.connector.Authenticator
import io.iohk.atala.prism.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.atala.prism.cmanager.models.requests.CreateStudent
import io.iohk.atala.prism.cmanager.models.Student
import io.iohk.atala.prism.cmanager.repositories.{CredentialsRepository, StudentsRepository}
import io.iohk.atala.prism.console.models.{Institution, IssuerGroup}
import io.iohk.atala.prism.protos.cmanager_api
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class StudentsServiceImpl(
    studentsRepository: StudentsRepository,
    credentialsRepository: CredentialsRepository,
    authenticator: Authenticator
)(implicit
    ec: ExecutionContext
) extends cmanager_api.StudentsServiceGrpc.StudentsService {

  override def createStudent(request: cmanager_api.CreateStudentRequest): Future[cmanager_api.CreateStudentResponse] = {
    def f(issuerId: Institution.Id) = {
      Future {
        val maybeGroupName =
          if (request.groupName.trim.isEmpty) None else Some(IssuerGroup.Name(request.groupName.trim))

        val model = request
          .into[CreateStudent]
          .withFieldConst(_.issuer, issuerId)
          .enableUnsafeOption
          .transform

        studentsRepository
          .createStudent(model, maybeGroupName)
          .map(studentToProto)
          .map(cmanager_api.CreateStudentResponse().withStudent)
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("createStudent", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }

  }

  override def getStudents(request: cmanager_api.GetStudentsRequest): Future[cmanager_api.GetStudentsResponse] = {
    def f(issuerId: Institution.Id) = {
      Future {
        val lastSeenStudent = Try(UUID.fromString(request.lastSeenStudentId)).map(Student.Id.apply).toOption
        val groupName = Option(request.groupName.trim).filter(_.nonEmpty).map(IssuerGroup.Name.apply)

        studentsRepository
          .getBy(issuerId, request.limit, lastSeenStudent, groupName)
          .map { list =>
            cmanager_api.GetStudentsResponse(list.map(studentToProto))
          }
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("getStudents", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }

  }

  override def getStudent(request: cmanager_api.GetStudentRequest): Future[cmanager_api.GetStudentResponse] = {
    def f(issuerId: Institution.Id) = {
      Future {
        val studentId = Student.Id(UUID.fromString(request.studentId))
        studentsRepository
          .find(issuerId, studentId)
          .map { maybe =>
            cmanager_api.GetStudentResponse(maybe.map(studentToProto))
          }
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("getStudent", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }

  }

  override def getStudentCredentials(
      request: cmanager_api.GetStudentCredentialsRequest
  ): Future[cmanager_api.GetStudentCredentialsResponse] = {
    def f(issuerId: Institution.Id) = {
      Future {
        val studentId = Student.Id(UUID.fromString(request.studentId))
        credentialsRepository
          .getUniversityCredentialsBy(issuerId, studentId)
          .map { list =>
            cmanager_api.GetStudentCredentialsResponse(list.map(universityCredentialToProto))
          }
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("getStudentCredentials", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }

  override def generateConnectionTokenForStudent(
      request: cmanager_api.GenerateConnectionTokenForStudentRequest
  ): Future[cmanager_api.GenerateConnectionTokenForStudentResponse] = {
    def f(issuerId: Institution.Id) = {
      Future {
        val studentId = Student.Id.apply(UUID.fromString(request.studentId))

        studentsRepository
          .generateToken(issuerId, studentId)
          .map(token => cmanager_api.GenerateConnectionTokenForStudentResponse(token.token))
          .value
          .map {
            case Right(x) => x
            case Left(e) => throw new RuntimeException(s"FAILED: $e")
          }
      }.flatten
    }

    authenticator.authenticated("generateConnectionTokenForStudent", request) { participantId =>
      f(Institution.Id(participantId.uuid))
    }
  }
}
