package io.iohk.atala.credentials

import io.iohk.atala.crypto.ECPublicKey

case class KeyData(
    publicKey: ECPublicKey,
    addedOn: TimestampInfo,
    revokedOn: Option[TimestampInfo]
)
