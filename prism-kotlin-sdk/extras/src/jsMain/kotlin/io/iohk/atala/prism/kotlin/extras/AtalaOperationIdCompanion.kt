package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.protos.SignedAtalaOperation

@JsExport
object AtalaOperationIdCompanion {
    fun of(atalaOperation: SignedAtalaOperation): AtalaOperationId =
        AtalaOperationId.of(atalaOperation)

    fun random(): AtalaOperationId =
        AtalaOperationId.random()

    fun fromHex(hex: String): AtalaOperationId =
        AtalaOperationId.fromHex(hex)
}
