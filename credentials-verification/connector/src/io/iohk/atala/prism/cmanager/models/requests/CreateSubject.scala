package io.iohk.atala.prism.cmanager.models.requests

import io.circe.Json
import io.iohk.atala.prism.cmanager.models.{Issuer, Subject}

case class CreateSubject(
    issuerId: Issuer.Id,
    externalId: Subject.ExternalId,
    data: Json
)
