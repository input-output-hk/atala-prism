package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.kotlin.identity.DID

case class RegisterDID(did: DID, name: String, logo: ParticipantLogo)
