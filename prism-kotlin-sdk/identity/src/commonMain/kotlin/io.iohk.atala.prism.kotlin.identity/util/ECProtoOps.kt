package io.iohk.atala.prism.kotlin.identity.util

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECPrivateKey
import io.iohk.atala.prism.kotlin.protos.AtalaOperation
import io.iohk.atala.prism.kotlin.protos.SignedAtalaOperation
import pbandk.ByteArr
import pbandk.encodeToByteArray
import kotlin.js.JsExport

@JsExport
object ECProtoOps {
    fun signedAtalaOperation(privateKey: ECPrivateKey, signedWith: String, atalaOperation: AtalaOperation): SignedAtalaOperation {
        val signature = EC.sign(atalaOperation.encodeToByteArray(), privateKey)
        return SignedAtalaOperation(
            signedWith = signedWith,
            signature = ByteArr(signature.getEncoded()),
            operation = atalaOperation
        )
    }
}
