package io.iohk.atala.prism.kotlin.credentials.content

import kotlinx.serialization.json.*
import kotlin.test.*

class CredentialContentTest {
    private val credential = CredentialContent(
        buildJsonObject {
            putJsonArray("credentialType") {
                add(1)
                add(2)
                add(3)
                add("test")
            }
            put("issuerDid", "did")
            put("issuanceKeyId", 123)
            put("issuance", false)
            putJsonObject("credentialSubject") {
                put("fieldName", true)
            }
        }
    )

    private val credentialString =
        """{"credentialType":[1,2,3,"test"],"issuerDid":"did","issuanceKeyId":123,"issuance":false,"credentialSubject":{"fieldName":true}}"""

    @Test
    fun testEncoding() {
        assertEquals(credentialString, credential.fields.toString())
    }

    @Test
    fun testDecoding() {
        assertEquals(credential, CredentialContent.fromString(credentialString))
    }

    @Test
    fun testAccessToString() {
        assertEquals("did", credential.getString("issuerDid"))
    }

    @Test
    fun testAccessToInt() {
        assertEquals(123, credential.getInt("issuanceKeyId"))
    }

    @Test
    fun testAccessToBoolean() {
        assertEquals(false, credential.getBoolean("issuance"))
    }

    @Test
    fun testAccessToArray() {
        val array = buildJsonArray {
            add(1)
            add(2)
            add(3)
            add("test")
        }
        assertEquals(array, credential.getArray("credentialType"))
    }

    @Test
    fun testAccessToNonexistentValue() {
        assertNull(credential.getString("nonexistent"))
    }

    @Test
    fun testAccessToEmptyField() {
        assertNull(credential.getString(""))
    }

    @Test
    fun testAccessToNestedPrimitive() {
        val nestedCredential = CredentialContent(
            buildJsonObject {
                putJsonObject("credentialSubject") {
                    put("fieldName", true)
                    putJsonObject("nested") {
                        put("key1", "value")
                        put("key2", 123)
                    }
                }
            }
        )
        assertEquals(true, nestedCredential.getBoolean("credentialSubject.fieldName"))
        assertEquals("value", nestedCredential.getString("credentialSubject.nested.key1"))
        assertEquals(123, nestedCredential.getInt("credentialSubject.nested.key2"))
    }

    @Test
    fun testAccessToNonexistentNestedValue() {
        assertNull(credential.getString("credentialSubject.nonexistent"))
        assertNull(credential.getString("nonexistent.nonexistent2.nonexistent3"))
    }

    @Test
    fun testBuilderSyntax() {
        val credentialContent = buildCredentialContent {
            put("key", "value")
        }

        assertEquals("value", credentialContent.getString("key"))
    }
}
