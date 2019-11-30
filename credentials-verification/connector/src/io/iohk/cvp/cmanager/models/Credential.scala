package io.iohk.cvp.cmanager.models

import java.time.{Instant, LocalDate}
import java.util.UUID

case class Credential(
    id: Credential.Id,
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

object Credential {

  case class Id(value: UUID) extends AnyVal
}
