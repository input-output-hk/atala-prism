package io.iohk.atala.prism.kotlin.credentials.exposed

import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.exposed.MerkleRootJS
import io.iohk.atala.prism.kotlin.crypto.exposed.SHA256DigestJS
import io.iohk.atala.prism.kotlin.crypto.exposed.toKotlin
import io.iohk.atala.prism.kotlin.identity.exposed.DIDSuffixJS
import io.iohk.atala.prism.kotlin.identity.exposed.toKotlin

fun CredentialBatchId.toJs(): CredentialBatchIdJS =
    CredentialBatchIdJS(id)

fun CredentialBatchIdJS.toKotlin(): CredentialBatchId =
    CredentialBatchId.fromString(id)!!

@JsExport
object CredentialBatchIdJSCompanion {
    @JsName("fromString")
    fun fromString(id: String): CredentialBatchIdJS? =
        CredentialBatchId.fromString(id)?.toJs()

    @JsName("fromDigest")
    fun fromDigest(digest: SHA256DigestJS): CredentialBatchIdJS =
        CredentialBatchId.fromDigest(digest.toKotlin()).toJs()

    @JsName("fromBatchData")
    fun fromBatchData(issuerDIDSuffix: DIDSuffixJS, merkleRoot: MerkleRootJS): CredentialBatchIdJS =
        CredentialBatchId.fromBatchData(issuerDIDSuffix.toKotlin(), merkleRoot.toKotlin()).toJs()

    @JsName("random")
    fun random(): CredentialBatchIdJS =
        CredentialBatchId.random().toJs()
}

@JsExport
data class CredentialBatchIdJS internal constructor(val id: String)
