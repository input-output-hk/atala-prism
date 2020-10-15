package io.iohk.atala.prism.cstore.grpc

import io.iohk.atala.prism.console.models.Contact
import io.iohk.atala.prism.console.models.Contact.ConnectionStatus
import io.iohk.atala.prism.cstore.models.{IndividualConnectionStatus, StoreIndividual}
import io.iohk.atala.prism.protos.cstore_models

object ProtoCodecs {
  def toConnectionStatusProto(status: IndividualConnectionStatus): cstore_models.IndividualConnectionStatus = {
    cstore_models.IndividualConnectionStatus
      .fromName(status.entryName)
      .getOrElse(throw new Exception(s"Unknown status: $status"))
  }

  def toIndividualProto(individual: StoreIndividual): cstore_models.Individual = {
    cstore_models.Individual(
      individualId = individual.id.uuid.toString,
      status = cstore_models.IndividualConnectionStatus
        .fromName(individual.status.entryName)
        .getOrElse(throw new Exception(s"Unknown status: ${individual.status}")),
      fullName = individual.fullName,
      connectionToken = individual.connectionToken.getOrElse(""),
      connectionId = individual.connectionId.fold("")(_.id.toString),
      email = individual.email.getOrElse("")
    )
  }

  def toHolderProto(holder: Contact): cstore_models.VerifierHolder = {
    val individualConnectionStatus = holder.connectionStatus match {
      case ConnectionStatus.InvitationMissing => IndividualConnectionStatus.Created
      case ConnectionStatus.ConnectionMissing => IndividualConnectionStatus.Invited
      case ConnectionStatus.ConnectionAccepted => IndividualConnectionStatus.Connected
      case ConnectionStatus.ConnectionRevoked => IndividualConnectionStatus.Revoked
    }

    cstore_models
      .VerifierHolder()
      .withHolderId(holder.contactId.value.toString)
      .withConnectionId(holder.connectionId.map(_.id.toString).getOrElse(""))
      .withConnectionToken(holder.connectionToken.fold("")(_.token))
      .withJsonData(holder.data.noSpaces)
      .withStatus(toConnectionStatusProto(individualConnectionStatus))
  }
}
