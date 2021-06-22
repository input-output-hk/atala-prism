package io.iohk.atala.prism.kotlin.extras

import com.benasher44.uuid.uuid4
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.util.BytesOps
import io.iohk.atala.prism.kotlin.protos.SignedAtalaOperation
import pbandk.encodeToByteArray
import kotlin.js.JsExport

@JsExport
data class AtalaOperationId(val digest: SHA256Digest) {
    fun value(): ByteArray = digest.value

    fun hexValue(): String = BytesOps.bytesToHex(value())

    companion object {
        fun of(atalaOperation: SignedAtalaOperation): AtalaOperationId {
            val hash = SHA256Digest.compute(atalaOperation.encodeToByteArray())
            return AtalaOperationId(hash)
        }

        fun random(): AtalaOperationId {
            val hash = SHA256Digest.compute(uuid4().toString().encodeToByteArray())
            return AtalaOperationId(hash)
        }

        fun fromHex(hex: String): AtalaOperationId {
            val hash = SHA256Digest.fromHex(hex)
            return AtalaOperationId(hash)
        }
    }
}
