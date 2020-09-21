package io.iohk.atala.prism.cmanager.models.requests

import java.time.LocalDate

import io.iohk.atala.prism.cmanager.models.Student
import io.iohk.atala.prism.console.models.Institution

case class CreateUniversityCredential(
    issuedBy: Institution.Id,
    studentId: Student.Id,
    title: String,
    enrollmentDate: LocalDate,
    graduationDate: LocalDate,
    groupName: String
)
