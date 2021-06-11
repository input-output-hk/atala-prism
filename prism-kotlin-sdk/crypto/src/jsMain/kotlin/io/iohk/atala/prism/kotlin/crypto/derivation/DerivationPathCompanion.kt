package io.iohk.atala.prism.kotlin.crypto.derivation

@JsExport
object DerivationPathCompanion {
    fun empty(): DerivationPath =
        DerivationPath.empty()

    fun fromPath(path: String): DerivationPath =
        DerivationPath.fromPath(path)
}
