package io.iohk.atala.prism.models

import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.protos.models.TimestampInfo

case class KeyData(
    issuingKey: ECPublicKey,
    addedOn: TimestampInfo,
    revokedOn: Option[TimestampInfo]
)
