package io.iohk.cvp.cmanager.models.requests

import io.circe.Json
import io.iohk.cvp.cmanager.models.{Issuer, IssuerGroup}

case class CreateSubject(
    issuerId: Issuer.Id,
    groupName: IssuerGroup.Name,
    data: Json
)
