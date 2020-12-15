package io.iohk.atala.prism.kotlin.crypto

import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair

expect object EC {
    fun generateKeyPair(): ECKeyPair
}
