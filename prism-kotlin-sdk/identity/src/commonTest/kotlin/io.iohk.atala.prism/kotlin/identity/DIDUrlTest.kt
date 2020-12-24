package io.iohk.atala.prism.kotlin.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DIDUrlTest {
    private val did = DID.buildPrismDID("abacaba")

    @Test
    fun testSupportAPlainValidDid() {
        assertEquals(DIDUrl(did, emptyList(), emptyMap(), null), DIDUrl.fromString(did.value))
    }

    @Test
    fun testSupportPaths() {
        val expectedDidUrl = DIDUrl(did, listOf("path1", "path2"), emptyMap(), null)
        assertEquals(expectedDidUrl, DIDUrl.fromString("${did.value}/path1/path2"))
    }

    @Test
    fun testSupportParameters() {
        val expectedDidUrl = DIDUrl(
            did,
            emptyList(),
            mapOf(Pair("param1", listOf("value1")), Pair("param2", listOf("value2"))),
            null
        )
        assertEquals(expectedDidUrl, DIDUrl.fromString("${did.value}?param1=value1&param2=value2"))
    }

    @Test
    fun testSupportFragments() {
        val expectedDidUrl = DIDUrl(did, emptyList(), emptyMap(), "fragment")
        assertEquals(expectedDidUrl, DIDUrl.fromString("${did.value}#fragment"))
    }

    @Test
    fun testSupportAllAtOnce() {
        val expectedDidUrl = DIDUrl(did, listOf("path"), mapOf(Pair("param", listOf("value"))), "fragment")
        assertEquals(expectedDidUrl, DIDUrl.fromString("${did.value}/path?param=value#fragment"))
    }

    @Test
    fun testFailOnInvalidUrls() {
        assertFailsWith<IllegalArgumentException> {
            DIDUrl.fromString("*!@#$%^&*()_+")
        }
    }

    @Test
    fun testFailOnEmtpyDidSuffix() {
        assertFailsWith<IllegalArgumentException> {
            DIDUrl.fromString("did:")
        }
    }

    @Test
    fun testFailOnInvalidDids() {
        val did = "did:notprism:abcdef"
        val didUrl = "$did/path1/path2"
        val caught = assertFailsWith<IllegalArgumentException> {
            DIDUrl.fromString(didUrl)
        }

        assertEquals("Invalid DID: $did", caught.message)
    }

    @Test
    fun testFailOnInvalidDidUrls() {
        val did = "did:prism:abcdef"
        val didUrl = "$did#fragment1#fragment2"
        val caught = assertFailsWith<IllegalArgumentException> {
            DIDUrl.fromString(didUrl)
        }

        assertEquals("Invalid DID URL: $didUrl", caught.message)
    }
}
