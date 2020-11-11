package io.iohk.atala.prism.credentials

import java.time.LocalDate

import io.iohk.atala.prism.identity.DID

case class CredentialContent[+S](
    credentialType: Seq[String],
    issuerDid: Option[DID],
    issuanceKeyId: Option[String],
    issuanceDate: Option[LocalDate],
    expiryDate: Option[LocalDate],
    credentialSubject: Option[S]
)
