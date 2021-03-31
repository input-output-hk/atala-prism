package io.iohk.atala.prism.console.models.actions

import io.circe.Json
import io.iohk.atala.prism.console.models.Contact.ExternalId
import io.iohk.atala.prism.console.models.IssuerGroup.Name

case class CreateContactsRequest(externalId: ExternalId, json: Json, groupName: Option[Name])
