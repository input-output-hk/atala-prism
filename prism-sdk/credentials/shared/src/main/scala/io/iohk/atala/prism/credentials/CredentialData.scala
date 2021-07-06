package io.iohk.atala.prism.credentials

case class CredentialData(
    issuedOn: TimestampInfo,
    revokedOn: Option[TimestampInfo]
)
