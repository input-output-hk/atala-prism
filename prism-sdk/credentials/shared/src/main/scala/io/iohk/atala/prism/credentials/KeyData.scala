package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.crypto.ECPublicKey

case class KeyData(
    publicKey: ECPublicKey,
    addedOn: TimestampInfo,
    revokedOn: Option[TimestampInfo]
)
