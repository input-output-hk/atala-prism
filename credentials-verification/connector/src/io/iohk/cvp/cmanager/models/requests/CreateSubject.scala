package io.iohk.cvp.cmanager.models.requests

import io.circe.Json
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup, Subject}

case class CreateSubject(
    issuerId: Issuer.Id,
    externalId: Subject.ExternalId,
    groupName: IssuerGroup.Name,
    data: Json
)
