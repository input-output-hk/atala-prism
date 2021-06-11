package io.iohk.atala.prism.kotlin.credentials

import com.benasher44.uuid.uuid4
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DIDSuffix
import io.iohk.atala.prism.kotlin.protos.CredentialBatchData
import pbandk.ByteArr
import pbandk.encodeToByteArray

data class CredentialBatchId private constructor(val id: String) {

    companion object {
        private val CREDENTIAL_BATCH_ID_RE = Regex("^[0-9a-f]{64}$")

        fun fromString(id: String): CredentialBatchId? =
            if (id.matches(CREDENTIAL_BATCH_ID_RE))
                CredentialBatchId(id)
            else
                null

        fun fromDigest(digest: SHA256Digest): CredentialBatchId =
            CredentialBatchId(digest.hexValue())

        fun fromBatchData(issuerDIDSuffix: DIDSuffix, merkleRoot: MerkleRoot): CredentialBatchId {
            val digest = SHA256Digest
                .compute(
                    CredentialBatchData(
                        issuerDid = issuerDIDSuffix.value,
                        merkleRoot = ByteArr(merkleRoot.hash.value)
                    ).encodeToByteArray()
                )

            return fromDigest(digest)
        }

        fun random(): CredentialBatchId =
            CredentialBatchId(
                SHA256Digest.compute(uuid4().toString().encodeToByteArray()).hexValue()
            )
    }
}
