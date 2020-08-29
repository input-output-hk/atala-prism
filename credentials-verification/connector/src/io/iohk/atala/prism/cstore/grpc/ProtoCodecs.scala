package io.iohk.atala.prism.cstore.grpc

import io.iohk.atala.prism.cstore.models.{IndividualConnectionStatus, StoreIndividual, VerifierHolder}
import io.iohk.prism.protos.cstore_models

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

  def toHolderProto(holder: VerifierHolder): cstore_models.VerifierHolder = {
    cstore_models
      .VerifierHolder()
      .withHolderId(holder.id.uuid.toString)
      .withConnectionId(holder.connectionId.map(_.id.toString).getOrElse(""))
      .withConnectionToken(holder.connectionToken.getOrElse(""))
      .withJsonData(holder.data.noSpaces)
      .withStatus(toConnectionStatusProto(holder.status))
  }
}
