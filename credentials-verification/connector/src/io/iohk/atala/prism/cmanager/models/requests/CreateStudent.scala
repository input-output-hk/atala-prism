package io.iohk.atala.prism.cmanager.models.requests

import java.time.LocalDate

import io.iohk.atala.prism.console.models.Institution

case class CreateStudent(
    issuer: Institution.Id,
    universityAssignedId: String,
    fullName: String,
    email: String,
    admissionDate: LocalDate
)
