package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.crypto.derivation.KeyType
import io.iohk.atala.prism.kotlin.crypto.derivation.MnemonicCode
import io.iohk.atala.prism.kotlin.crypto.signature.ECSignature
import io.iohk.atala.prism.kotlin.identity.DID.Companion.masterKeyId
import io.iohk.atala.prism.kotlin.identity.util.toProto
import io.iohk.atala.prism.kotlin.protos.*
import pbandk.encodeToByteArray
import kotlin.test.*

class DIDTest {
    private val mnemonicCodeDummy =
        MnemonicCode("shallow gadget world plug runway begin load bargain tomorrow never garment indoor".split(" "))
    private val stateHashDummy = "5f7802238f5d64a48fda6cc13a9467b2065248d31a94129ed0c0ea96d9b341a0"
    private val encodedStateDummy = "CmAKXhJcCgdtYXN0ZXIwEAFCTwoJc2VjcDI1NmsxEiD9401VbdKKeGfyGhHvZSEc-_nhfQg-8IPM2-QAOaZG-hog6ZugZN9_WLbX8xalkHa6YvuOJAWbGyQW66lumV0-vbM"

    @Test
    fun testCreateTheExpectedLongFormDid() {
        // The expected resulting DID
        val expectedDID = DID.buildPrismDID(
            stateHashDummy,
            encodedStateDummy
        )

        val didContext = DID.createDIDFromMnemonic(mnemonicCodeDummy, 0, "secret")
        assertEquals(expectedDID, didContext.unpublishedDID)
        assertEquals(masterKeyId, didContext.createDIDSignedOperation.signedWith)

        when (val format = didContext.unpublishedDID.getFormat()) {
            is LongForm -> assertEquals(expectedDID.suffix, format.validate().suffix())
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
            is Canonical -> assertEquals(canonicalSuffix, format.suffix)
            else -> fail("unexpected format for canonical DID")
        }
        when (val format = long.getFormat()) {
            is LongForm -> {
                assertEquals(canonicalSuffix, format.stateHash)
                assertEquals(encodedStateUsed, format.encodedState)
            }
            else -> fail("unexpected format for long DID")
        }
        when (wrong.getFormat()) {
            is Unknown -> {
                // do nothing, the test would fail on the other cases
            }
            else -> fail("unexpected format for unknown DID")
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
        val masterKey = EC.toPublicKey(xBytes, yBytes)

        val expectedInitialState =
            AtalaOperation(
                operation = AtalaOperation.Operation.CreateDid(
                    CreateDIDOperation(
                        didData = DIDData(
                            publicKeys = listOf(
                                PublicKey(
                                    id = masterKeyId,
                                    usage = KeyUsage.MASTER_KEY,
                                    keyData = PublicKey.KeyData.EcKeyData(masterKey.toProto())
                                )
                            )
                        )
                    )
                )
            )

        when (val format = long.getFormat()) {
            is LongForm -> assertEquals(expectedInitialState, format.validate().initialState)
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
    fun testSuffix() {
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

    @Test
    fun testUpdateDIDAtalaOperationShouldReturnSignedUpdateDIDOperation() {
        val singingKeyPair = EC.generateKeyPair()
        val singingKeyId = "signingKeyId"
        val did = DID.buildPrismDID(stateHashDummy)
        val previousHash = SHA256Digest.compute(byteArrayOf(123))
        val key1ToAdd = KeyInformation(
            keyId = "key1ToAdd",
            keyTypeEnum = KeyType.MASTER_KEY,
            publicKey = EC.generateKeyPair().publicKey
        )
        val key2ToAdd = KeyInformation(
            keyId = "key2ToAdd",
            keyTypeEnum = KeyType.ISSUING_KEY,
            publicKey = EC.generateKeyPair().publicKey
        )
        val keys1ToRevoke: String = "keys1ToRevoke"
        val keys2ToRevoke: String = "keys2ToRevoke"

        val expectedAtalaOperation: AtalaOperation = AtalaOperation(
            operation = AtalaOperation.Operation.UpdateDid(
                updateDid = UpdateDIDOperation(
                    previousOperationHash = pbandk.ByteArr(previousHash.value),
                    id = did.suffix.value,
                    actions = listOf(
                        UpdateDIDAction(
                            UpdateDIDAction.Action.AddKey(
                                AddKeyAction(
                                    PublicKey(
                                        id = key1ToAdd.keyId,
                                        usage = KeyUsage.fromName(KeyType.keyTypeToString(key1ToAdd.keyTypeEnum)),
                                        keyData = key1ToAdd.publicKey.toProto().let { PublicKey.KeyData.EcKeyData(it) }
                                    )
                                )
                            )
                        ),
                        UpdateDIDAction(
                            UpdateDIDAction.Action.AddKey(
                                AddKeyAction(
                                    PublicKey(
                                        id = key2ToAdd.keyId,
                                        usage = KeyUsage.fromName(KeyType.keyTypeToString(key2ToAdd.keyTypeEnum)),
                                        keyData = key2ToAdd.publicKey.toProto().let { PublicKey.KeyData.EcKeyData(it) }
                                    )
                                )
                            )
                        ),
                        UpdateDIDAction(
                            UpdateDIDAction.Action.RemoveKey(
                                RemoveKeyAction(keys1ToRevoke)
                            )
                        ),
                        UpdateDIDAction(
                            UpdateDIDAction.Action.RemoveKey(
                                RemoveKeyAction(keys2ToRevoke)
                            )
                        )
                    )
                )
            )
        )
        val expectedOperationHash = SHA256Digest.compute(expectedAtalaOperation.encodeToByteArray())

        val result = DID.updateDIDAtalaOperation(
            singingKeyPair.privateKey,
            singingKeyId,
            did,
            previousHash,
            listOf(key1ToAdd, key2ToAdd),
            listOf(keys1ToRevoke, keys2ToRevoke)
        )

        assertEquals(expectedAtalaOperation, result.updateDIDSignedOperation.operation)
        assertEquals(expectedOperationHash, result.operationHash)
        assertTrue(
            EC.verify(
                expectedAtalaOperation.encodeToByteArray(),
                singingKeyPair.publicKey,
                ECSignature(result.updateDIDSignedOperation.signature.array)
            )
        )
    }
}
