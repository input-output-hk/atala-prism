package io.iohk.atala.prism.cmanager.grpc.services.codecs

import java.time.LocalDate

import com.google.protobuf.ByteString
import io.iohk.atala.prism.cmanager.models.Student.ConnectionStatus
import io.iohk.atala.prism.cmanager.models.{GenericCredential, Student, Subject, UniversityCredential}
import io.iohk.prism.protos.common_models.Date
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

  implicit val studentConnectionStatus2Proto: Transformer[ConnectionStatus, cmanager_models.StudentConnectionStatus] = {
    case Student.ConnectionStatus.InvitationMissing => cmanager_models.StudentConnectionStatus.InvitationMissing
    case Student.ConnectionStatus.ConnectionMissing => cmanager_models.StudentConnectionStatus.ConnectionMissing
    case Student.ConnectionStatus.ConnectionAccepted => cmanager_models.StudentConnectionStatus.ConnectionAccepted
    case Student.ConnectionStatus.ConnectionRevoked => cmanager_models.StudentConnectionStatus.ConnectionRevoked
  }

  def universityCredentialToProto(credential: UniversityCredential): cmanager_models.CManagerCredential = {
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

  def toLocalDate(date: Date): LocalDate =
    LocalDate.of(date.year, date.month, date.day)

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
      // As part of ATA-2989, we decided to return an empty string to simplify the changes. This was agreed with the
      // front end team
      .withGroupName("")
  }

  def subjectToProto(subject: Subject): cmanager_models.IssuerSubject = {
    cmanager_models
      .IssuerSubject()
      .withId(subject.id.value.toString)
      .withExternalId(subject.externalId.value)
      .withConnectionStatus(studentConnectionStatus2Proto.transform(subject.connectionStatus))
      .withConnectionToken(subject.connectionToken.map(_.token).getOrElse(""))
      .withConnectionId(subject.connectionId.map(_.toString).getOrElse(""))
      // As part of ATA-2989, we decided to return an empty string to simplify the changes. This was agreed with the
      // front end team
      .withGroupName("")
      .withJsonData(subject.data.noSpaces)
  }

  def genericCredentialToProto(credential: GenericCredential): cmanager_models.CManagerGenericCredential = {
    val model = cmanager_models
      .CManagerGenericCredential()
      .withCredentialId(credential.credentialId.value.toString)
      .withIssuerId(credential.issuedBy.value.toString)
      .withSubjectId(credential.subjectId.value.toString)
      .withCredentialData(credential.credentialData.noSpaces)
      .withIssuerName(credential.issuerName)
      .withGroupName(credential.groupName)
      .withSubjectData(credential.subjectData.noSpaces)
      .withExternalId(credential.externalId.value)

    credential.publicationData.fold(model) { data =>
      model
        .withNodeCredentialId(data.nodeCredentialId)
        .withIssuanceOperationHash(ByteString.copyFrom(data.issuanceOperationHash.value))
        .withEncodedSignedCredential(data.encodedSignedCredential)
        .withPublicationStoredAt(data.storedAt.toEpochMilli)
    }
  }
}
