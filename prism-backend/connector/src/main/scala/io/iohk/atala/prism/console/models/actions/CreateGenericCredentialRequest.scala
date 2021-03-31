package io.iohk.atala.prism.console.models.actions

import io.circe.Json
import io.iohk.atala.prism.console.models.Contact.{ExternalId, Id}

case class CreateGenericCredentialRequest(
    maybeContactId: Option[Id],
    maybeExternalId: Option[ExternalId],
    credentialData: Json
)
