package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.util.toProto
import io.iohk.atala.prism.protos.*
import kotlin.test.*

@ExperimentalUnsignedTypes
class DIDTest {
    @Test
    fun testCreateTheExpectedLongFormDid() {
        // bytes extracted from a randomly generated key
        val xBytes = byteArrayOf(
            30, -105, -2, -54, 18, 39, -77, -30, -49, -34, -46, -17, 3, -56, -100, 11, -111, 73, -43,
            -103, 65, -63, -71, 101, 62, -46, 29, 53, -99, -22, -46, 53
        )
        val yBytes = byteArrayOf(
            -103, 81, -25, 85, 91, -109, -113, 111, 106, 7, -95, 3, 4, 36, 22, -11, -65, 126, -4,
            -116, -42, -90, -72, -118, 87, -120, 17, -119, 23, -77, -118, 69
        )
        val masterKey = EC.toPublicKey(xBytes.toList(), yBytes.toList())

        // The expected resulting DID
        val expectedDID = DID.buildPrismDID(
            "6d1cda2c1286622f41a5fe4a47ea6d8e3f5999dc38a6b7a893d55e0d75b569ce",
            "CmAKXhJcCgdtYXN0ZXIwEAFCTwoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRogmVHnVVuTj29qB6EDBCQW9b9-_IzWpriKV4gRiRezikU"
        )

        val did = DID.createUnpublishedDID(masterKey)
        assertEquals(expectedDID, did)

        when (val format = expectedDID.getFormat()) {
            is DIDFormat.LongForm -> assertEquals(expectedDID.suffix, format.validate().suffix())
            else -> fail("unexpected format for long DID")
        }
    }

    val canonicalSuffix = "0f753f41e0f3488ba56bd581d153ae9b3c9040cbcc7a63245b4644a265eb3b77"
    val encodedStateUsed =
        "CmEKXxJdCgdtYXN0ZXIwEAFCUAoJc2VjcDI1NmsxEiAel_7KEiez4s_e0u8DyJwLkUnVmUHBuWU-0h01nerSNRohAJlR51Vbk49vagehAwQkFvW_fvyM1qa4ileIEYkXs4pF"

    val short = DID.buildPrismDID(canonicalSuffix)
    val long = DID.buildPrismDID(canonicalSuffix, encodedStateUsed)
    val wrong = DID.buildPrismDID("wrong")

    @Test
    fun testGetTheCorrectCanonicalSuffix() {
        assertEquals(canonicalSuffix, short.getCanonicalSuffix()?.value)
        assertEquals(canonicalSuffix, long.getCanonicalSuffix()?.value)
        assertEquals(null, wrong.getCanonicalSuffix())
    }

    @Test
    fun testTellIfTheDidIsInCanonicalForm() {
        assertTrue(short.isCanonicalForm())
        assertFalse(long.isCanonicalForm())
        assertFalse(wrong.isCanonicalForm())
    }

    @Test
    fun testTellIfTheDidIsInLongForm() {
        assertFalse(short.isLongForm())
        assertTrue(long.isLongForm())
        assertFalse(wrong.isLongForm())
    }

    @Test
    fun testGetTheCorrectFormat() {
        when (val format = short.getFormat()) {
            is DIDFormat.Canonical -> assertEquals(canonicalSuffix, format.suffix)
            else -> fail("unexpected format for canonical DID")
        }
        when (val format = long.getFormat()) {
            is DIDFormat.LongForm -> {
                assertEquals(canonicalSuffix, format.stateHash)
                assertEquals(encodedStateUsed, format.encodedState)
            }
            else -> fail("unexpected format for long DID")
        }
        when (wrong.getFormat()) {
            is DIDFormat.Unknown -> {
                // do nothing, the test would fail on the other cases
            }
            else -> fail ("unexpected format for unknown DID")
        }
    }

    @Test
    fun testProperlyObtainTheDidSuffix() {
        assertEquals(canonicalSuffix, short.suffix.value)
        assertEquals("$canonicalSuffix:$encodedStateUsed", long.suffix.value)
    }

    @Test
    fun testProperlyStripThePrismDidPrefix() {
        assertEquals(canonicalSuffix, short.suffix.value)
        assertEquals("$canonicalSuffix:$encodedStateUsed", long.suffix.value)
    }

    fun testProperlyValidateALongFormDid() {
        // bytes extracted from a randomly generated key
        val xBytes = byteArrayOf(
            30, -105, -2, -54, 18, 39, -77, -30, -49, -34, -46, -17, 3, -56, -100, 11, -111, 73, -43,
            -103, 65, -63, -71, 101, 62, -46, 29, 53, -99, -22, -46, 53
        )
        val yBytes = byteArrayOf(
            -103, 81, -25, 85, 91, -109, -113, 111, 106, 7, -95, 3, 4, 36, 22, -11, -65, 126, -4,
            -116, -42, -90, -72, -118, 87, -120, 17, -119, 23, -77, -118, 69
        )
        val masterKey = EC.toPublicKey(xBytes.toList(), yBytes.toList())

        val expectedInitialState =
            AtalaOperation(
                operation = AtalaOperation.Operation.CreateDid(
                    CreateDIDOperation(
                        didData = DIDData(
                            publicKeys = listOf(
                                PublicKey(
                                    id = "master0",
                                    usage = KeyUsage.MASTER_KEY,
                                    keyData = PublicKey.KeyData.EcKeyData(masterKey.toProto())
                                )
                            )
                        )
                    )
                )
            )

        when (val format = long.getFormat()) {
            is DIDFormat.LongForm -> assertEquals(expectedInitialState, format.validate().initialState)
            else -> fail("Long form DID with unexpected format")
        }
    }

    @Test
    fun testBeCreatableFromTestPrefix() {
        val rawDid = "did:test:int-demo"
        val did = DID.fromString(rawDid)
        assertEquals(rawDid, did.value)
    }

    @Test
    fun testStripPrismPrefix() {
        val input = DID.buildPrismDID("aabbccddee")
        val expected = "aabbccddee"

        assertEquals(expected, input.suffix.value)
    }

    @Test
    fun testSucceedForValidDid() {
        val validDid = DID.buildPrismDID("aabbccddee")
        val unsafeDid = DID.fromString(validDid.value)

        assertEquals(validDid, unsafeDid)
    }

    @Test
    fun testFailForInvalidDid() {
        val caught = assertFailsWith<IllegalArgumentException> {
            DID.fromString("invalid-did")
        }

        assertEquals("Invalid DID: invalid-did", caught.message)
    }
}
