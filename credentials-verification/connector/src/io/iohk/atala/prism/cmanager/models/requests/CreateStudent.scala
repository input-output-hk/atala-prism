package io.iohk.atala.prism.cmanager.models.requests

import java.time.LocalDate

import io.iohk.atala.prism.cmanager.models.Issuer

case class CreateStudent(
    issuer: Issuer.Id,
    universityAssignedId: String,
    fullName: String,
    email: String,
    admissionDate: LocalDate
)
