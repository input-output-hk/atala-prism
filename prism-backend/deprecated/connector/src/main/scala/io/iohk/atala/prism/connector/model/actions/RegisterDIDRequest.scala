package io.iohk.atala.prism.connector.model.actions

import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

case class RegisterDIDRequest(
    name: String,
    didOrOperation: Either[DID, SignedAtalaOperation],
    tpe: ParticipantType,
    logo: ParticipantLogo
)
