package io.iohk.atala.prism.kotlin.crypto.derivation

@JsExport
object DerivationAxisCompanion {
    fun normal(num: Int): DerivationAxis =
        DerivationAxis.normal(num)

    fun hardened(num: Int): DerivationAxis =
        DerivationAxis.hardened(num)
}
