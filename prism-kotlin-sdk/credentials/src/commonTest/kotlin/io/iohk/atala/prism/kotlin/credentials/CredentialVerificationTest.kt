package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.MerkleTree
import io.iohk.atala.prism.kotlin.crypto.SHA256Digest
import io.iohk.atala.prism.kotlin.identity.DID
import kotlinx.datetime.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.*

class CredentialVerificationTest {

    private val unsignedCredential = JsonBasedCredential(
        CredentialContent(
            JsonObject(
                mapOf(
                    Pair("issuerDid", JsonPrimitive(DID.buildPrismDID("123456678abcdefg").value)),
                    Pair("issuanceKeyId", JsonPrimitive("Issuance-0"))
                )
            )
        )
    )

    private val before = TimestampInfo(Clock.System.now().minus(1, DateTimeUnit.SECOND).epochSeconds, 1, 1)
    private val now = TimestampInfo(Clock.System.now().epochSeconds, 2, 2)
    private val after = TimestampInfo(Clock.System.now().plus(1, DateTimeUnit.SECOND).epochSeconds, 3, 3)

    fun rootAndProofFor(cred: Credential): Pair<MerkleTree.MerkleRoot, MerkleTree.MerkleInclusionProof> {
        val (root, profs) = CredentialBatches.batch(listOf(cred))
        return Pair(root, profs.first())
    }

    @Test
    fun `succeed when valid`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = null)
        val credentialData = CredentialData(issuedOn = now, revokedOn = null)
        val signedCredential = unsignedCredential.sign(keys.privateKey)

        CredentialVerification.verify(keyData, credentialData, signedCredential)
    }

    @Test
    fun `fail when credential is revoked`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = null)
        val credentialData = CredentialData(issuedOn = now, revokedOn = after)
        val signedCredential = unsignedCredential.sign(keys.privateKey)

        assertFailsWith<CredentialWasRevoked> {
            CredentialVerification.verify(keyData, credentialData, signedCredential)
        }
    }

    @Test
    fun `fail when credential was added before key`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = now, revokedOn = null)
        val credentialData = CredentialData(issuedOn = before, revokedOn = null)
        val signedCredential = unsignedCredential.sign(keys.privateKey)

        assertFailsWith<KeyWasNotValid> {
            CredentialVerification.verify(keyData, credentialData, signedCredential)
        }
    }

    @Test
    fun `fail when key is revoked before credential is added`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = now)
        val credentialData = CredentialData(issuedOn = after, revokedOn = null)
        val signedCredential = unsignedCredential.sign(keys.privateKey)

        assertFailsWith<KeyWasRevoked> {
            CredentialVerification.verify(keyData, credentialData, signedCredential)
        }
    }

    @Test
    fun `fail when signature is invalid`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = null)
        val credentialData = CredentialData(issuedOn = now, revokedOn = null)
        // Sign with different key
        val signedCredential =
            unsignedCredential.sign(EC.generateKeyPair().privateKey)

        assertFailsWith<InvalidSignature> {
            CredentialVerification.verify(keyData, credentialData, signedCredential)
        }
    }

    @Test
    fun `succeed when valid (including merkle tree verification)`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = null)
        val batchData = BatchData(issuedOn = now, revokedOn = null)
        val signedCredential = unsignedCredential.sign(keys.privateKey)
        val (root, proof) = rootAndProofFor(signedCredential)
        val revokedAt: TimestampInfo? = null

        CredentialVerification
            .verify(
                keyData,
                batchData,
                revokedAt,
                root,
                proof,
                signedCredential
            )
    }

    @Test
    fun `fail when the credential was revoked`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = null)
        val batchData = BatchData(issuedOn = now, revokedOn = null)
        val signedCredential = unsignedCredential.sign(keys.privateKey)
        val (root, proof) = rootAndProofFor(signedCredential)
        val revokedAt: TimestampInfo? = now

        assertFailsWith<CredentialWasRevoked> {
            CredentialVerification
                .verify(
                    keyData,
                    batchData,
                    revokedAt,
                    root,
                    proof,
                    signedCredential
                )
        }
    }

    @Test
    fun `fail when credential batch is revoked`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = null)
        val batchData = BatchData(issuedOn = now, revokedOn = after)
        val signedCredential = unsignedCredential.sign(keys.privateKey)
        val (root, proof) = rootAndProofFor(signedCredential)
        val revokedAt: TimestampInfo? = null

        assertFailsWith<BatchWasRevoked> {
            CredentialVerification
                .verify(
                    keyData,
                    batchData,
                    revokedAt,
                    root,
                    proof,
                    signedCredential
                )
        }
    }

    @Test
    fun `fail when credential batch is issued before key is added`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = now, revokedOn = null)
        val batchData = BatchData(issuedOn = before, revokedOn = null)
        val signedCredential = unsignedCredential.sign(keys.privateKey)
        val (root, proof) = rootAndProofFor(signedCredential)
        val revokedAt: TimestampInfo? = null

        assertFailsWith<KeyWasNotValid> {
            CredentialVerification
                .verify(
                    keyData,
                    batchData,
                    revokedAt,
                    root,
                    proof,
                    signedCredential
                )
        }
    }

    @Test
    fun `fail when key is revoked before credential batch is issued`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = now)
        val batchData = BatchData(issuedOn = after, revokedOn = null)
        val signedCredential = unsignedCredential.sign(keys.privateKey)
        val (root, proof) = rootAndProofFor(signedCredential)
        val revokedAt: TimestampInfo? = null

        assertFailsWith<KeyWasRevoked> {
            CredentialVerification
                .verify(
                    keyData,
                    batchData,
                    revokedAt,
                    root,
                    proof,
                    signedCredential
                )
        }
    }

    @Test
    fun `fail when signature is invalid (merkle proof)`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = null)
        val batchData = BatchData(issuedOn = now, revokedOn = null)
        // Sign with different key
        val signedCredential = unsignedCredential.sign(EC.generateKeyPair().privateKey)
        val (root, proof) = rootAndProofFor(signedCredential)
        val revokedAt: TimestampInfo? = null

        assertFailsWith<InvalidSignature> {
            CredentialVerification
                .verify(
                    keyData,
                    batchData,
                    revokedAt,
                    root,
                    proof,
                    signedCredential
                )
        }
    }

    @Test
    fun `fail when merkle proof is invalid`() {
        val keys = EC.generateKeyPair()
        val keyData = KeyData(publicKey = keys.publicKey, addedOn = before, revokedOn = null)
        val batchData = BatchData(issuedOn = now, revokedOn = null)
        val signedCredential = unsignedCredential.sign(keys.privateKey)
        val (root, proof) = rootAndProofFor(signedCredential)
        val revokedAt: TimestampInfo? = null
        val rootWithDifferentHash = root.copy(hash = SHA256Digest(root.hash.value.drop(1)))

        assertFailsWith<InvalidMerkleProof> {
            CredentialVerification
                .verify(
                    keyData,
                    batchData,
                    revokedAt,
                    rootWithDifferentHash,
                    proof,
                    signedCredential
                )
        }
    }
}
