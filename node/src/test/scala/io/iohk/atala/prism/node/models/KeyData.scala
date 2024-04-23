package io.iohk.atala.prism.node.models

import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey
import io.iohk.atala.prism.protos.models.TimestampInfo

case class KeyData(
    issuingKey: SecpPublicKey,
    addedOn: TimestampInfo,
    revokedOn: Option[TimestampInfo]
)
