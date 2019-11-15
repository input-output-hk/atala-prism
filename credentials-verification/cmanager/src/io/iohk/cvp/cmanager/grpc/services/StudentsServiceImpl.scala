package io.iohk.cvp.cmanager.grpc.services

import io.iohk.cvp.cmanager.grpc.UserIdInterceptor
import io.iohk.cvp.cmanager.grpc.services.codecs.ProtoCodecs._
import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.repositories.StudentsRepository
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

class StudentsServiceImpl(studentsRepository: StudentsRepository)(implicit ec: ExecutionContext)
    extends protos.StudentsServiceGrpc.StudentsService {

  override def createStudent(request: protos.CreateStudentRequest): Future[protos.CreateStudentResponse] = {
    val userId = UserIdInterceptor.USER_ID_CTX_KEY.get()
    val model = request
      .into[CreateStudent]
      .withFieldConst(_.issuer, userId)
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
}
