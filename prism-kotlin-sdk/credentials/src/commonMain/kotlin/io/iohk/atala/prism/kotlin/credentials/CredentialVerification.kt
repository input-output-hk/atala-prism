package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.crypto.MerkleTree

sealed class VerificationException(message: String) : Exception(message)

data class CredentialWasRevoked(val revokedOn: TimestampInfo) : VerificationException("Credential was revoked on $revokedOn")

data class BatchWasRevoked(val revokedOn: TimestampInfo) : VerificationException("Batch was revoked on $revokedOn")

object InvalidMerkleProof : VerificationException("Invalid merkle proof")

data class KeyWasNotValid(val keyAddedOn: TimestampInfo, val credentialIssuedOn: TimestampInfo) :
    VerificationException("Key was no valid, key added on $keyAddedOn, credential issued on $credentialIssuedOn")

data class KeyWasRevoked(val credentialIssuedOn: TimestampInfo, val keyRevokedOn: TimestampInfo) :
    VerificationException("Key was revoked, credential issued on $credentialIssuedOn key revoked on $keyRevokedOn")

object InvalidSignature : VerificationException("Invalid signature")

object CredentialVerification {

    /**
     * This method receives data retrieved from the node and the credential to verify.
     *
     * We have some assumptions to call this method:
     * 1. The keyData is obtained from the PRISM node and corresponds to the key used to sign the credential
     * 2. The credentialData is obtained from the PRISM node and corresponds to the signedCredential parameter
     * 3. The issuer DID is a trusted one
     *
     * @param keyData the public key used to sign the credential and its addition and (optional)
     *                revocation timestamps
     * @param credentialData the credential information extracted from the node
     * @param credential the credential to verify
     *
     * @throws VerificationException
     */
    fun verify(
        keyData: KeyData,
        credentialData: CredentialData,
        credential: Credential
    ) {

        if (credentialData.revokedOn != null) {
            throw CredentialWasRevoked(revokedOn = credentialData.revokedOn)
        }

        if (!keyData.addedOn.occurredBefore(credentialData.issuedOn)) {
            throw KeyWasNotValid(keyAddedOn = keyData.addedOn, credentialIssuedOn = credentialData.issuedOn)
        }

        if (keyData.revokedOn != null && !credentialData.issuedOn.occurredBefore(keyData.revokedOn)) {
            throw KeyWasRevoked(credentialIssuedOn = credentialData.issuedOn, keyRevokedOn = keyData.revokedOn)
        }

        if (!credential.isValidSignature(keyData.publicKey)) {
            throw InvalidSignature
        }
    }

    /** This method receives data retrieved from the node and the credential to verify.
     *
     * We have some assumptions to call this method:
     * 1. The keyData is obtained from the PRISM node and corresponds to the key used to sign the credential
     * 2. The batchData is obtained from the PRISM node and corresponds to the signedCredential parameter
     * 3. The issuer DID is a trusted one
     * 4. The credentialRevocationTime is obtained from the PRISM node and corresponds to the signedCredential parameter
     *
     * @param keyData the public key used to sign the credential and its addition and (optional)
     *                revocation timestamps
     * @param batchData the credential information extracted from the node
     * @param credentialRevocationTime the credential information extracted from the node
     * @param merkleRoot merkle root that represents the batch
     * @param inclusionProof merkle proof of inclusion that states that signedCredential is in the batch
     * @param signedCredential the credential to verify
     *
     * @throws VerificationException
     */
    fun verify(
        keyData: KeyData,
        batchData: BatchData,
        credentialRevocationTime: TimestampInfo?,
        merkleRoot: MerkleTree.MerkleRoot,
        inclusionProof: MerkleTree.MerkleInclusionProof,
        signedCredential: Credential
    ) {

        if (batchData.revokedOn != null) {
            throw BatchWasRevoked(revokedOn = batchData.revokedOn)
        }

        if (!keyData.addedOn.occurredBefore(batchData.issuedOn)) {
            throw KeyWasNotValid(keyAddedOn = keyData.addedOn, credentialIssuedOn = batchData.issuedOn)
        }

        if (keyData.revokedOn != null && !batchData.issuedOn.occurredBefore(keyData.revokedOn)) {
            throw KeyWasRevoked(credentialIssuedOn = batchData.issuedOn, keyRevokedOn = keyData.revokedOn)
        }

        if (!signedCredential.isValidSignature(keyData.publicKey)) {
            throw InvalidSignature
        }

        if (credentialRevocationTime != null) {
            throw CredentialWasRevoked(credentialRevocationTime)
        }

        if (!CredentialBatches.verifyInclusion(signedCredential, merkleRoot, inclusionProof)) {
            throw InvalidMerkleProof
        }
    }
}
