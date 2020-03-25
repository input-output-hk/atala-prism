package io.iohk.cvp.cmanager.grpc.services.codecs

import java.time.LocalDate

import io.iohk.cvp.cmanager.models.{Credential, Student}
import io.iohk.prism.protos.{cmanager_models, common_models}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

object ProtoCodecs {
  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
  }

  implicit val studentConnectionStatus2Proto
      : Transformer[Student.ConnectionStatus, cmanager_models.StudentConnectionStatus] = {
    case Student.ConnectionStatus.InvitationMissing => cmanager_models.StudentConnectionStatus.InvitationMissing
    case Student.ConnectionStatus.ConnectionMissing => cmanager_models.StudentConnectionStatus.ConnectionMissing
    case Student.ConnectionStatus.ConnectionAccepted => cmanager_models.StudentConnectionStatus.ConnectionAccepted
    case Student.ConnectionStatus.ConnectionRevoked => cmanager_models.StudentConnectionStatus.ConnectionRevoked
  }

  def credentialToProto(credential: Credential): cmanager_models.CManagerCredential = {
    val graduationDate = credential.graduationDate.into[common_models.Date].transform
    val enrollmentDate = credential.enrollmentDate.into[common_models.Date].transform
    cmanager_models
      .CManagerCredential()
      .withId(credential.id.value.toString)
      .withIssuerId(credential.issuedBy.value.toString)
      .withStudentId(credential.studentId.value.toString)
      .withIssuerName(credential.issuerName)
      .withStudentName(credential.studentName)
      .withTitle(credential.title)
      .withGroupName(credential.groupName)
      .withEnrollmentDate(enrollmentDate)
      .withGraduationDate(graduationDate)
  }

  def studentToProto(student: Student): cmanager_models.Student = {
    cmanager_models
      .Student()
      .withId(student.id.value.toString)
      .withUniversityAssignedId(student.universityAssignedId)
      .withFullName(student.fullName)
      .withEmail(student.email)
      .withAdmissionDate(date2ProtoTransformer.transform(student.admissionDate))
      .withConnectionStatus(studentConnectionStatus2Proto.transform(student.connectionStatus))
      .withConnectionToken(student.connectionToken.map(_.token).getOrElse(""))
      .withConnectionId(student.connectionId.map(_.toString).getOrElse(""))
      .withGroupName(student.groupName.value)
  }
}
