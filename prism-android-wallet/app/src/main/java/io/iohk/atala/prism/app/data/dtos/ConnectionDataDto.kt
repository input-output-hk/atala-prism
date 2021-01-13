package io.iohk.atala.prism.app.data.dtos

import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair

/*
* TODO this needs to be removed to use Contact Model
* */
class ConnectionDataDto(val connectionId: String, val ecKeyPairFromPath: ECKeyPair) {
}