package io.iohk.atala.prism.console.models.actions

import io.iohk.atala.prism.console.models.{Contact, IssuerGroup}

case class UpdateGroupRequest(
    groupId: IssuerGroup.Id,
    contactsToAdd: List[Contact.Id],
    contactsToRemove: List[Contact.Id]
)
