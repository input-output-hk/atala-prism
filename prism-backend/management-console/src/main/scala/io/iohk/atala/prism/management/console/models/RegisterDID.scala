package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.protos.node_models

case class RegisterDID(signedOperation: node_models.SignedAtalaOperation, name: String, logo: ParticipantLogo)
