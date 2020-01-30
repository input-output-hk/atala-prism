package io.iohk.cvp.cmanager.grpc.services

import java.util.UUID

import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Student}
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
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class StudentsServiceImpl(studentsRepository: StudentsRepository, credentialsRepository: CredentialsRepository)(
    implicit ec: ExecutionContext
) extends protos.StudentsServiceGrpc.StudentsService {

  private val logger = LoggerFactory.getLogger(this.getClass)

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
      }.flatMap(identity)
    }

    Try {
      getIssuerId()
    } match {
      case Failure(ex) =>
        logger.info(s"createStudent - missing userId, request = ${request.toProtoString}", ex)
        Future.failed(throw new RuntimeException("Missing userId"))

      case Success(issuerId) =>
        val result = f(issuerId)
        result.onComplete {
          case Success(response) =>
            logger.info(
              s"createStudent, userId = $issuerId, request = ${request.toProtoString}, response = ${response.toProtoString}"
            )

          case Failure(ex) =>
            logger.info(s"createStudent failed, userId = $issuerId, request = ${request.toProtoString}", ex)
        }
        result
    }
  }

  override def getStudents(request: GetStudentsRequest): Future[GetStudentsResponse] = {
    val userId = getIssuerId()
    val lastSeenStudent = Try(UUID.fromString(request.lastSeenStudentId)).map(Student.Id.apply).toOption
    val groupName = Option(request.groupName.trim).filter(_.nonEmpty).map(IssuerGroup.Name.apply)

    studentsRepository
      .getBy(userId, request.limit, lastSeenStudent, groupName)
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
