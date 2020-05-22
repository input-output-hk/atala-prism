package io.iohk.cvp.cmanager.models.requests

import java.time.LocalDate

import io.iohk.cvp.cmanager.models.{Issuer, Student}

case class CreateUniversityCredential(
    issuedBy: Issuer.Id,
    studentId: Student.Id,
    title: String,
    enrollmentDate: LocalDate,
    graduationDate: LocalDate,
    groupName: String
)
