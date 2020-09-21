package io.iohk.atala.prism.cmanager.models

import java.time.{Instant, LocalDate}
import java.util.UUID

import io.iohk.atala.prism.console.models.Institution

case class UniversityCredential(
    id: UniversityCredential.Id,
    issuedBy: Institution.Id,
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
