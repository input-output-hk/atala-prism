package io.iohk.atala.prism.console.models.actions

import io.iohk.atala.prism.console.models.GenericCredential.Id

case class GetGenericCredentialsRequest(lastSeenCredential: Option[Id], limit: Int)
