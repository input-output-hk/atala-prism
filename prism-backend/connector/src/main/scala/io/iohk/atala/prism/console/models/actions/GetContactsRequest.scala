package io.iohk.atala.prism.console.models.actions

import io.iohk.atala.prism.console.models.Contact
import io.iohk.atala.prism.console.models.IssuerGroup.Name

case class GetContactsRequest(lastSeenContact: Option[Contact.Id], groupName: Option[Name])
