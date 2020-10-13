package io.iohk.atala.credentials

case class CredentialData(
    issuedOn: TimestampInfo,
    revokedOn: Option[TimestampInfo]
)
