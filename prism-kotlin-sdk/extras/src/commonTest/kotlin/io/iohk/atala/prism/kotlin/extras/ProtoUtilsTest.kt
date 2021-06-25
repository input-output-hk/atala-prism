package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.identity.util.ECProtoOps
import io.iohk.atala.prism.kotlin.protos.CredentialBatchData
import kotlinx.serialization.json.JsonObject
import pbandk.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtoUtilsTest {
    @Test
    fun createDidAtalaOperationWorks() {
        val keyPair = EC.generateKeyPair()
        val atalaOperation = ProtoUtils.createDidAtalaOperation(keyPair)
        assertEquals(1, atalaOperation.createDid?.didData?.publicKeys?.size)
    }

    @Test
    fun issueCredentialBatchOperationWorks() {
        val credentialBatchData = CredentialBatchData()
        val atalaOperation = ProtoUtils.issueCredentialBatchOperation(credentialBatchData)
        assertEquals(credentialBatchData, atalaOperation.issueCredentialBatch?.credentialBatchData)
    }

    @Test
    fun revokeCredentialsOperationWorks() {
        val batchOperationHash = SHA256Digest.compute(byteArrayOf(0))
        val batchId = CredentialBatchId.random()
        val credential = JsonBasedCredential(
            content = CredentialContent(JsonObject(emptyMap())),
            signature = ECSignature("signature".encodeToByteArray())
        )
        val atalaOperation = ProtoUtils.revokeCredentialsOperation(
            batchOperationHash,
            batchId,
            listOf(credential)
        )
        assertEquals(batchId.id, atalaOperation.revokeCredentials?.credentialBatchId)
    }

    @Test
    fun signedAtalaOperationWorks() {
        val keyPair = EC.generateKeyPair()
        val atalaOperation = ProtoUtils.createDidAtalaOperation(keyPair)
        val signedAtalaOperation = ECProtoOps.signedAtalaOperation(keyPair, "master0", atalaOperation)
        val operationBytes = atalaOperation.encodeToByteArray()
        val signature = EC.toSignature(signedAtalaOperation.signature.array)
        assertTrue(EC.verify(operationBytes, keyPair.publicKey, signature))
    }
}
