package io.iohk.atala.prism.console.grpc

import io.iohk.atala.prism.console.models
import io.iohk.atala.prism.console.models.Contact.ConnectionStatus
import io.iohk.atala.prism.protos.console_models

object ProtoCodecs {

  def toContactProto(contact: models.Contact): console_models.Contact = {
    val token = contact.connectionToken.fold("")(_.token)
    val connectionId = contact.connectionId.fold("")(_.id.toString)
    val status = contact.connectionStatus match {
      case ConnectionStatus.InvitationMissing => console_models.ContactConnectionStatus.INVITATION_MISSING
      case ConnectionStatus.ConnectionMissing => console_models.ContactConnectionStatus.CONNECTION_MISSING
      case ConnectionStatus.ConnectionAccepted => console_models.ContactConnectionStatus.CONNECTION_ACCEPTED
      case ConnectionStatus.ConnectionRevoked => console_models.ContactConnectionStatus.CONNECTION_REVOKED
    }

    console_models
      .Contact()
      .withContactId(contact.contactId.value.toString)
      .withExternalId(contact.externalId.value)
      .withJsonData(contact.data.noSpaces)
      .withConnectionStatus(status)
      .withConnectionToken(token)
      .withConnectionId(connectionId)
      .withCreatedAt(contact.createdAt.toEpochMilli)
  }
}
