package io.iohk.atala.prism.kotlin.identity.util

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.protos.AtalaOperation
import io.iohk.atala.prism.kotlin.protos.SignedAtalaOperation
import pbandk.ByteArr
import pbandk.encodeToByteArray

object ECProtoOps {
    fun signedAtalaOperation(ecKeyPair: ECKeyPair, signedWith: String, atalaOperation: AtalaOperation): SignedAtalaOperation {
        val signature = EC.sign(atalaOperation.encodeToByteArray(), ecKeyPair.privateKey)
        return SignedAtalaOperation(
            signedWith = signedWith,
            signature = ByteArr(signature.getEncoded()),
            operation = atalaOperation
        )
    }
}
