package io.iohk.atala.prism.management.console.models

import io.iohk.atala.prism.kotlin.identity.PrismDid

case class RegisterDID(did: PrismDid, name: String, logo: ParticipantLogo)
