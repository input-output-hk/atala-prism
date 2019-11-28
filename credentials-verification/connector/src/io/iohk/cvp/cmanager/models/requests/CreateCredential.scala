package io.iohk.cvp.cmanager.models.requests

import java.time.LocalDate

import io.iohk.cvp.cmanager.models.Issuer

case class CreateCredential(
    issuedBy: Issuer.Id,
    subject: String,
    title: String,
    enrollmentDate: LocalDate,
    graduationDate: LocalDate,
    groupName: String
)
