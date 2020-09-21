package io.iohk.atala.prism.cmanager.models.requests

import io.circe.Json
import io.iohk.atala.prism.console.models.{Contact, Institution}

case class CreateGenericCredential(
    issuedBy: Institution.Id,
    subjectId: Contact.Id,
    credentialData: Json,
    groupName: String
)
