package io.iohk.atala.prism.kotlin.crypto

expect object SHA256 {
    fun compute(bytes: ByteArray): ByteArray
}
