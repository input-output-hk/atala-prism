package io.iohk.cvp.viewmodel.dtos

import io.iohk.atala.prism.crypto.japi.ECKeyPair

class ConnectionDataDto(val connectionId: String, val ecKeyPairFromPath: ECKeyPair) {
}