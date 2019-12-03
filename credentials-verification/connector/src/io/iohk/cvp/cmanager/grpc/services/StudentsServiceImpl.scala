package io.iohk.cvp.cmanager.grpc.services

import java.util.UUID

import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.cvp.cmanager.models.Student
import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.protos.{
  GenerateConnectionTokenRequest,
  GenerateConnectionTokenResponse,
  GetStudentCredentialsRequest,
  GetStudentCredentialsResponse,
  GetStudentRequest,
  GetStudentResponse,
  GetStudentsRequest,
  GetStudentsResponse
}
import io.iohk.cvp.cmanager.repositories.{CredentialsRepository, StudentsRepository}
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class StudentsServiceImpl(studentsRepository: StudentsRepository, credentialsRepository: CredentialsRepository)(
    implicit ec: ExecutionContext
) extends protos.StudentsServiceGrpc.StudentsService {

  override def createStudent(request: protos.CreateStudentRequest): Future[protos.CreateStudentResponse] = {
    val issuerId = getIssuerId()
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
  }

  override def getStudents(request: GetStudentsRequest): Future[GetStudentsResponse] = {
    val userId = getIssuerId()
    val lastSeenStudent = Try(UUID.fromString(request.lastSeenStudentId)).map(Student.Id.apply).toOption
    studentsRepository
      .getBy(userId, request.limit, lastSeenStudent)
      .map { list =>
        protos.GetStudentsResponse(list.map(studentToProto))
      }
      .value
      .map {
        case Right(x) => x
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }

  override def getStudent(request: GetStudentRequest): Future[GetStudentResponse] = {
    val userId = getIssuerId()
    val studentId = Student.Id(UUID.fromString(request.studentId))
    studentsRepository
      .find(userId, studentId)
      .map { maybe =>
        protos.GetStudentResponse(maybe.map(studentToProto))
      }
      .value
      .map {
        case Right(x) => x
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }

  override def getStudentCredentials(request: GetStudentCredentialsRequest): Future[GetStudentCredentialsResponse] = {
    val userId = getIssuerId()
    val studentId = Student.Id(UUID.fromString(request.studentId))
    credentialsRepository
      .getBy(userId, studentId)
      .map { list =>
        protos.GetStudentCredentialsResponse(list.map(credentialToProto))
      }
      .value
      .map {
        case Right(x) => x
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }

  override def generateConnectionToken(
      request: GenerateConnectionTokenRequest
  ): Future[GenerateConnectionTokenResponse] = {
    val issuerId = getIssuerId()
    val studentId = Student.Id.apply(UUID.fromString(request.studentId))

    studentsRepository
      .generateToken(issuerId, studentId)
      .map(token => protos.GenerateConnectionTokenResponse(token.token))
      .value
      .map {
        case Right(x) => x
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }
}
