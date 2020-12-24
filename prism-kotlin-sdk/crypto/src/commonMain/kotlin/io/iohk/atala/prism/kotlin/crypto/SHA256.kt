package io.iohk.atala.prism.kotlin.crypto

@ExperimentalUnsignedTypes
expect object SHA256 {
    fun compute(bytes: UByteArray): UByteArray
}
