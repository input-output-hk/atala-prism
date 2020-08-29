package io.iohk.atala.prism.cmanager.models

import java.time.{Instant, LocalDate}
import java.util.UUID

case class UniversityCredential(
    id: UniversityCredential.Id,
    issuedBy: Issuer.Id,
    studentId: Student.Id,
    title: String,
    enrollmentDate: LocalDate,
    graduationDate: LocalDate,
    groupName: String,
    createdOn: Instant,
    issuerName: String,
    studentName: String
)

object UniversityCredential {

  case class Id(value: UUID) extends AnyVal
}
