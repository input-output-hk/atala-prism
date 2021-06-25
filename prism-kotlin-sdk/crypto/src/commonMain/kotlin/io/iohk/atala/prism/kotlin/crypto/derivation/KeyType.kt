package io.iohk.atala.prism.kotlin.crypto.derivation

import kotlin.js.JsExport

typealias KeyTypeEnum = Int

// TODO: This smell-code is because Kotlin/JS doesn't support interfaces and enum classes now
//  change to enum class once kotlin/js fix the issue
@JsExport
object KeyType {
    private val values: List<String> = listOf("MASTER_KEY", "ISSUING_KEY", "COMMUNICATION_KEY", "AUTHENTICATION_KEY")

    val MASTER_KEY: Int = 0
    val ISSUING_KEY: Int = 1
    val COMMUNICATION_KEY: Int = 2
    val AUTHENTICATION_KEY: Int = 3

    fun keyTypeToString(key: KeyTypeEnum): String {
        require(0 <= key && key < values.size)
        return values[key]
    }
}
