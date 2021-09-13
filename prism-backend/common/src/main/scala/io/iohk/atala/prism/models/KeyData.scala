package io.iohk.atala.prism.models

import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.protos.models.TimestampInfo

case class KeyData(issuingKey: ECPublicKey, addedOn: TimestampInfo, revokedOn: Option[TimestampInfo])
