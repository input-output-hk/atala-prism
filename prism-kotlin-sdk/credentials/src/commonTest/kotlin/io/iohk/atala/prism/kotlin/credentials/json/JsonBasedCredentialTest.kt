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
    fun reconstructTheOriginalCredentialFormSignedString() {
        assertEquals(
            JsonBasedCredential.fromString(signedCredentialString),
            JsonBasedCredential(
                content = emptyCredentialContent,
                signature = ECSignature("signature".encodeToByteArray().toList().map { it.toUByte() })
            )
        )
    }

    @Test
    fun allowToParseCustomCredentialSubject() {
        val credential = JsonBasedCredential.fromString(customCredential)

        assertEquals(
            credential.content.getField("credentialSubject"),
            JsonObject(mapOf(Pair("id", JsonPrimitive(1))))
        )
    }

    @Test
    fun failToConstructWhenBytesAreNotFromAValidJSON() {
        assertFails {
            JsonBasedCredential.fromString("invalid")
        }
    }

    @Test
    fun signCredential() {
        val unsignedCredential = JsonBasedCredential(content = emptyCredentialContent)

        assertTrue(unsignedCredential.sign(keys.privateKey).isValidSignature(keys.publicKey))
    }

    @Test
    fun computeCanonicalForm() {
        val unsignedCredential = JsonBasedCredential.fromString(emptyCredential)
        val signedCredential = unsignedCredential.sign(keys.privateKey)

        assertEquals(unsignedCredential.canonicalForm, emptyCredential)
        assertTrue(signedCredential.canonicalForm.startsWith("e30.")) // the signature is dynamic
    }
}
