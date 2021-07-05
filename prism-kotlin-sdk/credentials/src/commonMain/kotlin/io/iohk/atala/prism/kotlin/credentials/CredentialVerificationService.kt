package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.credentials.common.Validated
import io.iohk.atala.prism.kotlin.credentials.utils.toECPublicKey
import io.iohk.atala.prism.kotlin.credentials.utils.toTimestampInfoModel
import io.iohk.atala.prism.kotlin.crypto.Hash
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.protos.*
import kotlin.js.JsExport

typealias VE = VerificationError

@JsExport
data class DIDDocument(val didData: DIDData) {
    fun getPublicKey(issuingKeyId: String): PublicKey? =
        didData.publicKeys.find { it.id == issuingKeyId }
}

class CredentialVerificationService(private val nodeService: NodeServiceCoroutine) {
    suspend fun verify(
        signedCredential: PrismCredential,
        merkleInclusionProof: MerkleInclusionProof
    ): VerificationResult {
        val issuerDidValidated: Validated<DID, VE> = extractValidIssuerDid(signedCredential)

        val getDidDocumentResponseValidated: Validated<GetDidDocumentResponse, VE> =
            issuerDidValidated.suspendableFlatMap { fetchDIDDocumentResponse(it) }

        val didDocumentValidated: Validated<DIDDocument, VE> = Validated.Applicative.apply(
            issuerDidValidated,
            getDidDocumentResponseValidated,
        ) { did, response -> getDidDocumentResponseValidated.flatMap { extractDIDDocument(response, did) } }

        val issuanceKeyIdValidated: Validated<String, VE> = extractValidIssuanceKeyId(signedCredential)

        val issuanceKeyValidated: Validated<PublicKey, VE> = Validated.Applicative.apply(
            didDocumentValidated,
            issuanceKeyIdValidated
        ) { didDocument, issuanceKey -> extractValidIssuingKeyDetailFromDIDDocument(didDocument, issuanceKey) }

        val issuanceKeyDetailValidated: Validated<ECPublicKey, VE> = Validated.Applicative.apply(
            issuanceKeyIdValidated,
            issuanceKeyValidated
        ) { issuanceKeyId, issuanceKey -> parseValidIssuingKeyDetail(issuanceKeyId, issuanceKey) }

        val issuingKeyAddedOnValidated: Validated<TimestampInfo, VE> = Validated.Applicative.apply(
            issuanceKeyIdValidated,
            issuanceKeyValidated
        ) { issuanceKeyId, issuanceKey -> extractValidIssuingKeyAddedOnTimestamp(issuanceKeyId, issuanceKey) }

        val issuingKeyRevokedOnValidated: Validated<TimestampInfo?, VE> =
            issuanceKeyValidated.map { extractIssuingKeyRevocationTimestamp(it) }

        val batchIdValidated: Validated<CredentialBatchId, VE> =
            issuerDidValidated.map { CredentialBatchId.fromBatchData(it.suffix, merkleInclusionProof.derivedRoot()) }

        val getBatchStateResponseValidated: Validated<GetBatchStateResponse, VE> =
            batchIdValidated.suspendableFlatMap { fetchBatchState(it) }

        val batchLedgerData: Validated<LedgerData, VE> = Validated.Applicative.apply(
            batchIdValidated,
            getBatchStateResponseValidated
        ) { batchId, getBatchStateResponse ->
            extractBatchPublicationLedgerData(
                getBatchStateResponse,
                batchId
            )
        }

        val batchIssuedOnValidated: Validated<TimestampInfo, VE> =
            Validated.Applicative.apply(batchLedgerData, batchIdValidated) { batchState, batchId ->
                extractValidBatchIssuedOnTimestamp(
                    batchState,
                    batchId
                )
            }

        val batchRevokedOnValidated: Validated<TimestampInfo?, VE> =
            getBatchStateResponseValidated.map { extractBatchRevocationTimestamp(it) }

        val getCredentialRevocationTimeResponseValidated: Validated<GetCredentialRevocationTimeResponse, VE> =
            batchIdValidated.suspendableMap { fetchGetCredentialRevocationTimeResponse(it, signedCredential) }

        val credentialRevocationTimeValidated: Validated<TimestampInfo?, VE> =
            getCredentialRevocationTimeResponseValidated.suspendableMap { extractCredentialRevocationTime(it) }

        val merkleProofValidated: Validated<Unit, VE> = validateMerkleProof(signedCredential, merkleInclusionProof)

        val credentialSignatureValidated: Validated<Unit, VE> =
            issuanceKeyDetailValidated.flatMap { validateCredentialSignature(signedCredential, it) }

        val batchNotRevokedValidated: Validated<Unit, VE> =
            batchRevokedOnValidated.flatMap { validateBatchNotRevoked(it) }

        val keyAddedBeforeIssuanceValidated: Validated<Unit, VE> = Validated.Applicative.apply(
            issuingKeyAddedOnValidated,
            batchIssuedOnValidated
        ) { issuingKey, batchIssuedOn -> validateKeyAddedBeforeIssuance(issuingKey, batchIssuedOn) }

        val keyAddedTimestampNotEqualToIssuanceValidated: Validated<Unit, VE> = Validated.Applicative.apply(
            issuingKeyAddedOnValidated,
            batchIssuedOnValidated
        ) { issuingKey, batchIssuedOn -> validateKeyAddedTimestampNotEqualToIssuance(issuingKey, batchIssuedOn) }

        val keyNotRevokedValidated: Validated<Unit, VE> = Validated.Applicative.apply(
            issuingKeyRevokedOnValidated,
            batchIssuedOnValidated
        ) { keyRevokedOn, batchIssuedOn -> validateKeyNotRevoked(keyRevokedOn, batchIssuedOn) }

        val keyRevokedTimestampNotEqualToIssuanceValidated: Validated<Unit, VE> = Validated.Applicative.apply(
            issuingKeyRevokedOnValidated,
            batchIssuedOnValidated
        ) { keyRevokedOn, batchIssuedOn ->
            validateKeyRevokedTimestampNotEqualToBatchIssuedOn(
                keyRevokedOn,
                batchIssuedOn
            )
        }

        val credentialNotRevokedValidated: Validated<Unit, VE> =
            credentialRevocationTimeValidated.flatMap { validateCredentialNotRevoked(it) }

        val lastSyncBlockTimestamp = listOfNotNull(
            getDidDocumentResponseValidated.result?.lastSyncedBlockTimestamp,
            getBatchStateResponseValidated.result?.lastSyncedBlockTimestamp,
            getCredentialRevocationTimeResponseValidated.result?.lastSyncedBlockTimestamp
        ).firstOrNull()

        return VerificationResult(
            listOf(
                issuerDidValidated,
                didDocumentValidated,
                issuanceKeyIdValidated,
                issuanceKeyValidated,
                issuanceKeyDetailValidated,
                issuingKeyAddedOnValidated,
                issuingKeyRevokedOnValidated,
                batchIdValidated,
                batchIssuedOnValidated,
                batchRevokedOnValidated,
                credentialRevocationTimeValidated,
                merkleProofValidated,
                credentialSignatureValidated,
                batchNotRevokedValidated,
                keyAddedBeforeIssuanceValidated,
                keyAddedTimestampNotEqualToIssuanceValidated,
                keyNotRevokedValidated,
                keyRevokedTimestampNotEqualToIssuanceValidated,
                credentialNotRevokedValidated
            ).mapNotNull { it.error }
                .distinct(),
            lastSyncBlockTimestamp
        )
    }

    private suspend fun fetchDIDDocumentResponse(did: DID): Validated<GetDidDocumentResponse, VE> =
        Validated.Valid(nodeService.GetDidDocument(GetDidDocumentRequest(did = did.value)))

    private fun extractDIDDocument(
        response: GetDidDocumentResponse,
        did: DID
    ): Validated<DIDDocument, VE> =
        getOrVerificationError(
            response.document?.let { DIDDocument(it) },
            VerificationError.IssuerDidDocumentNotFoundOnChain(did)
        )

    private suspend fun fetchBatchState(batchId: CredentialBatchId): Validated<GetBatchStateResponse, VE> =
        Validated.Valid(nodeService.GetBatchState(GetBatchStateRequest(batchId = Hash.fromHex(batchId.id).hexValue())))

    private fun extractBatchPublicationLedgerData(
        getBatchStateResponse: GetBatchStateResponse,
        batchId: CredentialBatchId
    ): Validated<LedgerData, VE> {
        return getOrVerificationError(
            getBatchStateResponse.publicationLedgerData,
            VerificationError.BatchNotFoundOnChain(batchId.id)
        )
    }

    private suspend fun fetchGetCredentialRevocationTimeResponse(
        batchId: CredentialBatchId,
        signedCredential: PrismCredential
    ): GetCredentialRevocationTimeResponse =
        nodeService.GetCredentialRevocationTime(
            GetCredentialRevocationTimeRequest(
                batchId = Hash.fromHex(batchId.id).hexValue(),
                credentialHash = pbandk.ByteArr(signedCredential.hash().value)
            )
        )

    private fun extractCredentialRevocationTime(
        response: GetCredentialRevocationTimeResponse?,
    ): TimestampInfo? =
        response?.revocationLedgerData?.timestampInfo?.toTimestampInfoModel()

    private fun validateMerkleProof(
        signedCredential: PrismCredential,
        merkleInclusionProof: MerkleInclusionProof
    ): Validated<Unit, VE> =
        validate(
            CredentialBatches.verifyInclusion(
                signedCredential,
                merkleInclusionProof.derivedRoot(),
                merkleInclusionProof
            ),
            VerificationError.InvalidMerkleProof
        )

    private fun extractValidIssuerDid(
        signedCredential: PrismCredential,
    ): Validated<DID, VE> =
        getOrVerificationError(signedCredential.content.getIssuerDid(), VerificationError.IssuerDidNotFoundInCredential)

    private fun extractValidIssuanceKeyId(
        signedCredential: PrismCredential,
    ): Validated<String, VE> =
        getOrVerificationError(
            signedCredential.content.getIssuanceKeyId(),
            VerificationError.IssuerKeyNotFoundInCredential
        )

    private fun extractValidIssuingKeyDetailFromDIDDocument(
        didDocument: DIDDocument,
        issuingKeyId: String
    ): Validated<PublicKey, VE> =
        getOrVerificationError(
            didDocument.getPublicKey(issuingKeyId),
            VerificationError.IssuingKeyIdNotFoundOnChain(issuingKeyId, didDocument)
        )

    private fun parseValidIssuingKeyDetail(
        issuingKeyId: String,
        publicKey: PublicKey
    ): Validated<ECPublicKey, VE> =
        getOrVerificationError(
            publicKey.toECPublicKey(),
            VerificationError.OnChainIssuingKeyCannotBeParsed(issuingKeyId, publicKey)
        )

    private fun extractValidIssuingKeyAddedOnTimestamp(
        issuingKeyId: String,
        publicKey: PublicKey
    ): Validated<TimestampInfo, VE> =
        getOrVerificationError(
            publicKey.addedOn?.toTimestampInfoModel(),
            VerificationError.IssuanceKeyPublicationTimestampNotFoundOnChain(issuingKeyId, publicKey)
        )

    private fun extractIssuingKeyRevocationTimestamp(
        publicKey: PublicKey
    ): TimestampInfo? = publicKey.revokedOn?.toTimestampInfoModel()

    private fun validateCredentialSignature(
        signedCredential: PrismCredential,
        issuanceKeyDetail: ECPublicKey
    ): Validated<Unit, VE> = validate(
        signedCredential.isValidSignature(issuanceKeyDetail),
        VerificationError.InvalidSignature(signedCredential, issuanceKeyDetail)
    )

    private fun extractValidBatchIssuedOnTimestamp(
        batchLedgerData: LedgerData?,
        batchId: CredentialBatchId
    ): Validated<TimestampInfo, VE> =
        getOrVerificationError(
            batchLedgerData?.timestampInfo?.toTimestampInfoModel(),
            VerificationError.BatchPublicationTimestampNotFoundOnChain(batchId.id)
        )

    private fun extractBatchRevocationTimestamp(
        batchStateResponse: GetBatchStateResponse,
    ): TimestampInfo? = batchStateResponse.revocationLedgerData?.timestampInfo?.toTimestampInfoModel()

    private fun validateBatchNotRevoked(maybeBatchRevokedOn: TimestampInfo?): Validated<Unit, VE> =
        maybeBatchRevokedOn?.let { Validated.Invalid(VerificationError.BatchWasRevokedOn(it)) } ?: Validated.Valid(Unit)

    private fun validateKeyAddedBeforeIssuance(
        keyAddedOn: TimestampInfo,
        batchIssuedOn: TimestampInfo
    ): Validated<Unit, VE> =
        validate(
            !batchIssuedOn.occurredBefore(keyAddedOn),
            VerificationError.KeyAddedAfterIssuance(keyAddedOn = keyAddedOn, batchIssuedOn = batchIssuedOn)
        )

    private fun validateKeyAddedTimestampNotEqualToIssuance(
        keyAddedOn: TimestampInfo,
        batchIssuedOn: TimestampInfo
    ): Validated<Unit, VE> =
        validate(
            batchIssuedOn != keyAddedOn,
            VerificationError.KeyAddedTimestampEqualsIssuance(keyAddedOn = keyAddedOn, batchIssuedOn = batchIssuedOn)
        )

    private fun validateKeyNotRevoked(
        keyRevokedOn: TimestampInfo?,
        batchIssuedOn: TimestampInfo
    ): Validated<Unit, VE> = keyRevokedOn?.let {
        validate(
            batchIssuedOn.occurredBefore(keyRevokedOn),
            VerificationError.KeyWasRevoked(keyRevokedOn = keyRevokedOn, batchIssuedOn = batchIssuedOn)
        )
    } ?: Validated.Valid(Unit)

    private fun validateKeyRevokedTimestampNotEqualToBatchIssuedOn(
        keyRevokedOn: TimestampInfo?,
        batchIssuedOn: TimestampInfo
    ): Validated<Unit, VE> = keyRevokedOn?.let {
        validate(
            batchIssuedOn != keyRevokedOn,
            VerificationError.KeyRevokedTimestampEqualsIssuance(
                keyRevokedOn = keyRevokedOn,
                batchIssuedOn = batchIssuedOn
            )
        )
    } ?: Validated.Valid(Unit)

    private fun validateCredentialNotRevoked(maybeCredentialRevokedOn: TimestampInfo?): Validated<Unit, VE> =
        maybeCredentialRevokedOn?.let { Validated.Invalid(VerificationError.CredentialWasRevokedOn(it)) }
            ?: Validated.Valid(Unit)

    private fun <T> getOrVerificationError(
        t: T?,
        verificationError: VE
    ): Validated<T, VE> {
        return t?.let { Validated.Valid(it) } ?: Validated.Invalid(verificationError)
    }

    private fun validate(
        validationPredicate: Boolean,
        verificationError: VE
    ): Validated<Unit, VE> {
        return if (validationPredicate) Validated.Valid(Unit) else Validated.Invalid(verificationError)
    }
}

@JsExport
data class VerificationResult(val verificationErrors: List<VE>, val lastSyncBlockTimestamp: pbandk.wkt.Timestamp?)

@JsExport
sealed class VerificationError(val errorMessage: String) {

    object InvalidMerkleProof : VerificationError("Invalid merkle proof")

    object IssuerDidNotFoundInCredential : VerificationError("Issuer DID not found in credential")

    object IssuerKeyNotFoundInCredential : VerificationError("Issuer Key not found in credential")

    data class IssuerDidDocumentNotFoundOnChain(val did: DID) :
        VerificationError("Issuer DID Document not found for DID:$did.value")

    data class IssuingKeyIdNotFoundOnChain(val issuerKeyId: String, val didDocument: DIDDocument) :
        VerificationError("Issuing Key not found on chain. issuingKeyId=$issuerKeyId didDocument=$didDocument")

    data class OnChainIssuingKeyCannotBeParsed(val issuerKeyId: String, val publicKey: PublicKey) :
        VerificationError("Unable to parse on chain Issuing Key. issuingKeyId=$issuerKeyId publicKey=$publicKey")

    data class IssuanceKeyPublicationTimestampNotFoundOnChain(val issuerKeyId: String, val publicKey: PublicKey) :
        VerificationError("Issuance Key publication timestamp not found on chain. issuingKeyId=$issuerKeyId publicKey=$publicKey")

    data class InvalidSignature(val signedCredential: PrismCredential, val publicKey: ECPublicKey) :
        VerificationError("Invalid signature. credential=$signedCredential publicKey=$publicKey")

    data class BatchNotFoundOnChain(val batchId: String) :
        VerificationError("Batch not found on chain. BatchId:$batchId")

    data class BatchPublicationTimestampNotFoundOnChain(val batchId: String) :
        VerificationError("Publication timestamp not found on chain. BatchId:$batchId")

    data class BatchWasRevokedOn(val timestamp: TimestampInfo) :
        VerificationError("Batch was revoked. RevokedOn:$timestamp")

    data class KeyAddedAfterIssuance(val keyAddedOn: TimestampInfo, val batchIssuedOn: TimestampInfo) :
        VerificationError("Invalid key. Key added after issuance. keyAddedOn:$keyAddedOn batchIssuedOn:$batchIssuedOn")

    data class KeyAddedTimestampEqualsIssuance(val keyAddedOn: TimestampInfo, val batchIssuedOn: TimestampInfo) :
        VerificationError("Key added time should never be equal to issuance time. keyAddedOn:$keyAddedOn batchIssuedOn:$batchIssuedOn")

    data class KeyWasRevoked(val keyRevokedOn: TimestampInfo, val batchIssuedOn: TimestampInfo) :
        VerificationError("Key was revoked before credential issuance. keyRevokedOn:$keyRevokedOn batchIssuedOn:$batchIssuedOn")

    data class KeyRevokedTimestampEqualsIssuance(val keyRevokedOn: TimestampInfo, val batchIssuedOn: TimestampInfo) :
        VerificationError("Key revoked time should never be equal to issuance time. keyRevokedOn:$keyRevokedOn batchIssuedOn:$batchIssuedOn")

    data class CredentialWasRevokedOn(val timestamp: TimestampInfo) :
        VerificationError("Credential was revoked. RevokedOn:$timestamp")
}
