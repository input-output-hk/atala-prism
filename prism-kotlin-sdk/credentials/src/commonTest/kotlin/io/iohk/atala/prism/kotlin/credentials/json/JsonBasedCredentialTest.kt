package io.iohk.atala.prism.kotlin.credentials.json

import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonBasedCredentialTest {
    val keys = EC.generateKeyPair()

    val emptyCredential = "{}"
    val emptyCredentialContent = CredentialContent(JsonObject(emptyMap()))

    val signedCredentialString = "e30=.c2lnbmF0dXJl" // {}.signature

    // { "credentialSubject": { "id": 1 } }.signature
    val customCredential = "eyAiY3JlZGVudGlhbFN1YmplY3QiOiB7ICJpZCI6IDEgfSB9.c2lnbmF0dXJl"

    @Test
    @ExperimentalUnsignedTypes
    fun `reconstruct the original credential form signed string`() {
        assertEquals(
            JsonBasedCredential.fromString(signedCredentialString),
            JsonBasedCredential(
                content = emptyCredentialContent,
                signature = ECSignature("signature".encodeToByteArray().toList().map { it.toUByte() })
            )
        )
    }

    @Test
    @ExperimentalUnsignedTypes
    fun `allow to parse custom credential subject`() {
        val credential = JsonBasedCredential.fromString(customCredential)

        assertEquals(
            credential.content.getField("credentialSubject"),
            JsonObject(mapOf(Pair("id", JsonPrimitive(1))))
        )
    }

    @Test
    @ExperimentalUnsignedTypes
    fun `fail to construct when bytes are not from a valid JSON`() {
        assertFails {
            JsonBasedCredential.fromString("invalid")
        }
    }

    @Test
    @ExperimentalUnsignedTypes
    fun `sign credential`() {
        val unsignedCredential = JsonBasedCredential(content = emptyCredentialContent)

        assertTrue(unsignedCredential.sign(keys.privateKey).isValidSignature(keys.publicKey))
    }

    @Test
    @ExperimentalUnsignedTypes
    fun `compute canonical form`() {
        val unsignedCredential = JsonBasedCredential.fromString(emptyCredential)
        val signedCredential = unsignedCredential.sign(keys.privateKey)

        assertEquals(unsignedCredential.canonicalForm, emptyCredential)
        assertTrue(signedCredential.canonicalForm.startsWith("e30.")) // the signature is dynamic
    }
}
