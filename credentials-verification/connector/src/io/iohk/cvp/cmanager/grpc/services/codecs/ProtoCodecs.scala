package io.iohk.cvp.cmanager.grpc.services.codecs

import java.time.LocalDate

import io.iohk.cvp.cmanager.models.{Credential, Student}
import io.iohk.cvp.cmanager.protos
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

object ProtoCodecs {
  implicit val proto2DateTransformer: Transformer[protos.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val date2ProtoTransformer: Transformer[LocalDate, protos.Date] = date => {
    protos.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
  }

  implicit val studentConnectionStatus2Proto: Transformer[Student.ConnectionStatus, protos.StudentConnectionStatus] = {
    case Student.ConnectionStatus.InvitationMissing => protos.StudentConnectionStatus.InvitationMissing
    case Student.ConnectionStatus.ConnectionMissing => protos.StudentConnectionStatus.ConnectionMissing
    case Student.ConnectionStatus.ConnectionAccepted => protos.StudentConnectionStatus.ConnectionAccepted
    case Student.ConnectionStatus.ConnectionRevoked => protos.StudentConnectionStatus.ConnectionRevoked
  }

  def credentialToProto(credential: Credential): protos.Credential = {
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

  def studentToProto(student: Student): protos.Student = {
    protos
      .Student()
      .withId(student.id.value.toString)
      .withIssuerId(student.issuer.value.toString)
      .withUniversityAssignedId(student.universityAssignedId)
      .withFullName(student.fullName)
      .withEmail(student.email)
      .withAdmissionDate(date2ProtoTransformer.transform(student.admissionDate))
      .withConnectionStatus(studentConnectionStatus2Proto.transform(student.connectionStatus))
      .withConnectionToken(student.connectionToken.map(_.token).getOrElse(""))
      .withConnectionId(student.connectionId.map(_.toString).getOrElse(""))
  }
}
