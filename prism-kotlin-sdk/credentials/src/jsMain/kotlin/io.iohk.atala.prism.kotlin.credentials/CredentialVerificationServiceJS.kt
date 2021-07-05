package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.protos.NodeServicePromise
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

@JsExport
class CredentialVerificationServiceJS(private val nodeService: NodeServicePromise) {
    private val credentialVerificationService = CredentialVerificationService(nodeService.internalService)
    fun verify(
        signedCredential: PrismCredential,
        merkleInclusionProof: MerkleInclusionProof
    ): Promise<VerificationResult> {
        return GlobalScope.promise {
            credentialVerificationService.verify(signedCredential, merkleInclusionProof)
        }
    }
}
