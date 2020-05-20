package io.iohk.cvp.cstore.grpc

import io.iohk.cvp.cstore.models.{IndividualConnectionStatus, VerifierHolder}
import io.iohk.prism.protos.cstore_models

object ProtoCodecs {
  def toConnectionStatusProto(status: IndividualConnectionStatus): cstore_models.IndividualConnectionStatus = {
    cstore_models.IndividualConnectionStatus
      .fromName(status.entryName)
      .getOrElse(throw new Exception(s"Unknown status: $status"))
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
