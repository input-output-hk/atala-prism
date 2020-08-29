package io.iohk.atala.prism.cmanager.models.requests

import io.circe.Json
import io.iohk.atala.prism.cmanager.models.{Issuer, Subject}

case class CreateGenericCredential(
    issuedBy: Issuer.Id,
    subjectId: Subject.Id,
    credentialData: Json,
    groupName: String
)
