package io.iohk.atala.prism.credentials

import java.time.LocalDate

case class CredentialContent[+S](
    credentialType: Seq[String],
    issuerDid: Option[String],
    issuanceKeyId: Option[String],
    issuanceDate: Option[LocalDate],
    expiryDate: Option[LocalDate],
    credentialSubject: Option[S]
)
