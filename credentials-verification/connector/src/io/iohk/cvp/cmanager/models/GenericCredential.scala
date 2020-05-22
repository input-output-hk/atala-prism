package io.iohk.cvp.cmanager.models

import java.time.Instant
import java.util.UUID

import io.circe.Json

case class GenericCredential(
    credentialId: GenericCredential.Id,
    issuedBy: Issuer.Id,
    subjectId: Subject.Id,
    credentialData: Json,
    groupName: String,
    createdOn: Instant,
    issuerName: String,
    subjectData: Json
)

object GenericCredential {
  case class Id(value: UUID) extends AnyVal
}
