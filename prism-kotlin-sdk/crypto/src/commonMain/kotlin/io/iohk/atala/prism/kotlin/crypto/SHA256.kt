package io.iohk.atala.prism.kotlin.crypto

expect object SHA256 {
    fun compute(bytes: List<Byte>): List<Byte>
}
