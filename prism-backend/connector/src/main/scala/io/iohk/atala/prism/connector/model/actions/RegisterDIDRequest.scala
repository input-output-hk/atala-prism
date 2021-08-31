package io.iohk.atala.prism.connector.model.actions

import io.iohk.atala.prism.connector.model.{ParticipantLogo, ParticipantType}
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation

case class RegisterDIDRequest(
    name: String,
    didOrOperation: Either[PrismDid, SignedAtalaOperation],
    tpe: ParticipantType,
    logo: ParticipantLogo
)
