package io.iohk.cvp.viewmodel.dtos

import io.iohk.atala.crypto.japi.ECKeyPair

class ConnectionDataDto(val connectionId: String, val ecKeyPairFromPath: ECKeyPair) {
}