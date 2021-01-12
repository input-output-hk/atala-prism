package io.iohk.atala.prism.app.viewmodel.dtos

import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair

class ConnectionDataDto(val connectionId: String, val ecKeyPairFromPath: ECKeyPair) {
}