package io.iohk.atala.prism.credentials

case class BatchData(
    issuedOn: TimestampInfo,
    revokedOn: Option[TimestampInfo]
)
