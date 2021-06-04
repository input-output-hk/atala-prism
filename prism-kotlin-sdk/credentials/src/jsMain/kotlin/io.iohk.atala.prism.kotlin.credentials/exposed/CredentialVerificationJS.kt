package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.*
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleInclusionProofJS
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleRootJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toKotlin

@JsExport
sealed class VerificationErrorJS(message: String) : Exception(message)

@JsExport
data class CredentialWasRevokedJS(val revokedOn: TimestampInfoJS) : VerificationErrorJS("Credential was revoked on $revokedOn")

@JsExport
data class BatchWasRevokedJS(val revokedOn: TimestampInfoJS) : VerificationErrorJS("Batch was revoked on $revokedOn")

@JsExport
object InvalidMerkleProofJS : VerificationErrorJS("Invalid merkle proof")

@JsExport
data class KeyWasNotValidJS(val keyAddedOn: TimestampInfoJS, val credentialIssuedOn: TimestampInfoJS) :
    VerificationErrorJS("Key was no valid, key added on $keyAddedOn, credential issued on $credentialIssuedOn")

@JsExport
data class KeyWasRevokedJS(val credentialIssuedOn: TimestampInfoJS, val keyRevokedOn: TimestampInfoJS) :
    VerificationErrorJS("Key was revoked, credential issued on $credentialIssuedOn key revoked on $keyRevokedOn")

@JsExport
object InvalidSignatureJS : VerificationErrorJS("Invalid signature")

@JsExport
sealed class VerificationResult {
    class Valid() : VerificationResult()
    class Invalid(val error: VerificationErrorJS) : VerificationResult()
}

@JsExport
object CredentialVerificationJS {
    private fun wrapVerificationException(block: () -> Unit): VerificationResult {
        return try {
            block()
            VerificationResult.Valid()
        } catch (e: VerificationException) {
            VerificationResult.Invalid(
                when (e) {
                    is CredentialWasRevoked -> CredentialWasRevokedJS(e.revokedOn.toJs())
                    is BatchWasRevoked -> BatchWasRevokedJS(e.revokedOn.toJs())
                    is InvalidMerkleProof -> InvalidMerkleProofJS
                    is KeyWasNotValid -> KeyWasNotValidJS(e.keyAddedOn.toJs(), e.credentialIssuedOn.toJs())
                    is KeyWasRevoked -> KeyWasRevokedJS(e.credentialIssuedOn.toJs(), e.keyRevokedOn.toJs())
                    is InvalidSignature -> InvalidSignatureJS
                }
            )
        }
    }

    fun verify(
        keyData: KeyDataJS,
        credentialData: CredentialDataJS,
        credential: CredentialJS
    ): VerificationResult =
        wrapVerificationException {
            CredentialVerification.verify(keyData.toKeyData(), credentialData.toCredentialData(), credential.credential)
        }

    fun verifyMerkle(
        keyData: KeyDataJS,
        batchData: BatchDataJS,
        credentialRevocationTime: TimestampInfoJS?,
        merkleRoot: MerkleRootJS,
        inclusionProof: MerkleInclusionProofJS,
        signedCredential: CredentialJS
    ): VerificationResult =
        wrapVerificationException {
            CredentialVerification.verify(
                keyData.toKeyData(),
                batchData.toBatchData(),
                credentialRevocationTime?.internal,
                merkleRoot.toKotlin(),
                MerkleInclusionProof(
                    SHA256Digest.fromHex(inclusionProof.hash),
                    inclusionProof.index,
                    inclusionProof.siblings.map { SHA256Digest.fromHex(it) }
                ),
                signedCredential.credential
            )
        }
}
