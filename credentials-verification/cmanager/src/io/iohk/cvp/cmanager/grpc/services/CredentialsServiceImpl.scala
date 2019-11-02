package io.iohk.cvp.cmanager.grpc.services

import java.time.LocalDate

import io.iohk.cvp.cmanager.grpc.UserIdInterceptor
import io.iohk.cvp.cmanager.models.Credential
import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.protos
import io.iohk.cvp.cmanager.repositories.CredentialsRepository
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

import scala.concurrent.{ExecutionContext, Future}

class CredentialsServiceImpl(credentialsRepository: CredentialsRepository)(implicit ec: ExecutionContext)
    extends protos.CredentialsServiceGrpc.CredentialsService {

  override def createCredential(request: protos.CreateCredentialRequest): Future[protos.CreateCredentialResponse] = {
    val userId = UserIdInterceptor.USER_ID_CTX_KEY.get()
    val model = request
      .into[CreateCredential]
      .withFieldConst(_.issuedBy, userId)
      .enableUnsafeOption
      .transform

    credentialsRepository
      .create(model)
      .map(toProto)
      .map(protos.CreateCredentialResponse().withCredential)
      .value
      .map {
        case Right(x) => x
        case Left(e) => throw new RuntimeException(s"FAILED: $e")
      }
  }

  override def getCredentials(request: protos.GetCredentialsRequest): Future[protos.GetCredentialsResponse] = {
    Future.failed(new Exception("Not implemented yet"))
  }

  private def toProto(credential: Credential): protos.Credential = {
    val graduationDate = credential.graduationDate.into[protos.Date].transform
    val enrollmentDate = credential.enrollmentDate.into[protos.Date].transform
    protos
      .Credential()
      .withId(credential.id.value.toString)
      .withSubject(credential.subject)
      .withTitle(credential.title)
      .withIssuedBy(credential.issuedBy.value.toString)
      .withGroupName(credential.groupName)
      .withEnrollmentDate(graduationDate)
      .withGraduationDate(enrollmentDate)
  }

  private implicit val proto2DateTransformer: Transformer[protos.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  private implicit val date2ProtoTransformer: Transformer[LocalDate, protos.Date] = date => {
    protos.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
  }
}
