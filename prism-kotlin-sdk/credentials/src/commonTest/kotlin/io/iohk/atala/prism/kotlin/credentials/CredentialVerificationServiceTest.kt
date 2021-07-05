package io.iohk.atala.prism.kotlin.credentials

import io.iohk.atala.prism.kotlin.credentials.common.runThenAssert
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.credentials.utils.toTimestampInfoModel
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.identity.util.toProto
import io.iohk.atala.prism.kotlin.protos.*
import io.iohk.atala.prism.kotlin.protos.TimestampInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pbandk.wkt.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals

class CredentialVerificationServiceTest {

    private val masterKeyId = "masterKeyId"
    private val masterKeyPair = EC.generateKeyPair()

    private val issuingKeyId = "issuingKeyId"
    private val issuingKeyPair = EC.generateKeyPair()

    private val defaultDid = DID.buildPrismDID("123456678abcdefg")

    private val nowInstant = Clock.System.now()
    private val beforeInstant = nowInstant.minus(1, DateTimeUnit.SECOND)
    private val afterInstant = nowInstant.plus(1, DateTimeUnit.SECOND)

    private val before = TimestampInfo(
        blockTimestamp = Timestamp(beforeInstant.epochSeconds, beforeInstant.nanosecondsOfSecond)
    )
    private val now = TimestampInfo(
        blockTimestamp = Timestamp(nowInstant.epochSeconds, nowInstant.nanosecondsOfSecond)
    )
    private val after = TimestampInfo(
        blockTimestamp = Timestamp(afterInstant.epochSeconds, afterInstant.nanosecondsOfSecond)
    )

    @Test
    fun testVerifyShouldReturnAnEmptyVerificationErrorListGivenHappyPathAndKeyNeverRevoked() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse() },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(listOf(), after.blockTimestamp),
                    verificationResult,
                    "VerificationError list should be empty"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnAnEmptyVerificationErrorListGivenHappyPathAndKeyWasRevokedAfterCredentialIssuance() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse(issuedKeyAddedOn = before, issuedKeyRevokedOn = after) },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = now, issuedOn = now) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(listOf(), now.blockTimestamp),
                    verificationResult,
                    "VerificationError list should be empty"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnIssuerDidNotFoundInCredentialGivenIssuerDidNotPresentInCredential() {

        val unsignedCredential = JsonBasedCredential(
            CredentialContent(
                JsonObject(
                    mapOf(
                        Pair("keyId", JsonPrimitive(issuingKeyId))
                    )
                )
            )
        )
        val signedCredential = unsignedCredential.sign(issuingKeyPair.privateKey)

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse() },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(listOf(VerificationError.IssuerDidNotFoundInCredential), null),
                    verificationResult,
                    "VerificationErrors should contain IssuerDIDNotFoundInCredential"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnIssuerDidDocumentNotFoundOnChainGivenDidDocumentWasNotPresentInDidDataFetchedFromChain() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { GetDidDocumentResponse(null) },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(VerificationError.IssuerDidDocumentNotFoundOnChain(defaultDid)),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain IssuerDIDDocumentNotFoundOnChain"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnIssuerKeyNotFoundInCredentialGivenIssuerKeyNotPresentInCredential() {

        val unsignedCredential = JsonBasedCredential(
            CredentialContent(
                JsonObject(
                    mapOf(
                        Pair(
                            "id",
                            JsonPrimitive(defaultDid.value)
                        )
                    )
                )
            )
        )
        val signedCredential = unsignedCredential.sign(issuingKeyPair.privateKey)

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse() },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(listOf(VerificationError.IssuerKeyNotFoundInCredential), after.blockTimestamp),
                    verificationResult,
                    "VerificationErrors should contain IssuerKeyNotFoundInCredential"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnIssuingKeyIdNotFoundOnChainGivenKeyNotPresentInDidDocument() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val getDidDocumentResponse = setupGetDidDocumentResponse(issuingPublicKey = null)

        val nodeService = setupNodeService(
            getDidDocument = { getDidDocumentResponse },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.IssuingKeyIdNotFoundOnChain(
                                issuingKeyId,
                                DIDDocument(getDidDocumentResponse.document!!)
                            )
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain IssuingKeyIdNotFoundOnChain"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnOnChainIssuingKeyCannotBeParsedGivenPublicCantBeConvertedToECPublicKey() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val issuerPublicKeyResponse = setupPublicKeyResponse(issuingKeyId, null, addedOn = now)
        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse(issuingPublicKey = issuerPublicKeyResponse) },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.OnChainIssuingKeyCannotBeParsed(
                                issuingKeyId,
                                issuerPublicKeyResponse
                            )
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain OnChainIssuingKeyCannotBeParsed"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnIssuanceKeyPublicationTimestampNotFoundOnChainGivenTimestampNotPresentInDidDocument() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val issuingKey = setupPublicKeyResponse(issuingKeyId, issuingKeyPair.publicKey, addedOn = null)
        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse(issuingPublicKey = issuingKey) },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.IssuanceKeyPublicationTimestampNotFoundOnChain(
                                issuingKeyId,
                                issuingKey
                            )
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain IssuanceKeyPublicationTimestampNotFoundOnChain"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnBatchNotFoundOnChainGivenNoBatchWasFoundForGivenBatchId() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse() },
            getBatchState = {
                setupGetBatchStateResponse(
                    lastSyncedBlockTimestamp = before,
                    publicationLedgerData = null
                )
            },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.BatchNotFoundOnChain(
                                CredentialBatchId.fromBatchData(
                                    defaultDid.suffix,
                                    merkleInclusionProof.derivedRoot()
                                ).id
                            )
                        ),
                        before.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain BatchNotFoundOnChain"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnBatchPublicationTimestampNotFoundOnChainGivenTimestampNoInBatchData() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse() },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = null) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.BatchPublicationTimestampNotFoundOnChain(
                                CredentialBatchId.fromBatchData(
                                    defaultDid.suffix,
                                    merkleInclusionProof.derivedRoot()
                                ).id
                            )
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain BatchPublicationTimestampNotFoundOnChain"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnInvalidMerkleProofGivenProofIsInvalid() {

        val unsignedCredential = setupDefaultJsonBasedCredential()
        val signedCredential = unsignedCredential.sign(issuingKeyPair.privateKey)

        val signedWithWrongKeyCredential = unsignedCredential.sign(EC.generateKeyPair().privateKey)
        val (_, merkleInclusionProof) = rootAndProofFor(signedWithWrongKeyCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse() },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.InvalidMerkleProof
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain InvalidMerkleProof"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnInvalidSignatureGivenCredentialHasInvalidSignature() {

        val unsignedCredential = setupDefaultJsonBasedCredential()

        val signedCredential = unsignedCredential.sign(issuingKeyPair.privateKey)

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse() },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        val signedWithWrongKeyCredential = unsignedCredential.sign(EC.generateKeyPair().privateKey)

        runThenAssert(
            { verificationService.verify(signedWithWrongKeyCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.InvalidMerkleProof,
                            VerificationError.InvalidSignature(signedWithWrongKeyCredential, issuingKeyPair.publicKey)
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain InvalidSignature"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnBatchWasRevokedOnGivenBatchHasRevocationTimestamp() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse() },
            getBatchState = {
                setupGetBatchStateResponse(
                    lastSyncedBlockTimestamp = now,
                    issuedOn = now,
                    revokedOn = after
                )
            },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.BatchWasRevokedOn(after.toTimestampInfoModel())
                        ),
                        now.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain BatchWasRevokedOn"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnKeyAddedAfterIssuanceGivenKeyTimestampIsAfterBatchTimestamp() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse(issuedKeyAddedOn = after) },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = now) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.KeyAddedAfterIssuance(
                                after.toTimestampInfoModel(),
                                now.toTimestampInfoModel()
                            )
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain KeyAddedAfterIssuance"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldNotReturnKeyAddedAfterIssuanceGivenKeyTimestampIsEqualToBatchTimestamp() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse(issuedKeyAddedOn = now) },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = now) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.KeyAddedTimestampEqualsIssuance(
                                now.toTimestampInfoModel(),
                                now.toTimestampInfoModel()
                            )
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain KeyAddedTimestampEqualsIssuance"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnKeyWasRevokedGivenKeyHasRevocationTimestampBeforeCredentialIssuance() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse(issuedKeyAddedOn = before, issuedKeyRevokedOn = now) },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.KeyWasRevoked(
                                now.toTimestampInfoModel(),
                                after.toTimestampInfoModel()
                            )
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain KeyWasRevoked"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnKeyWasRevokedGivenKeyHasRevocationTimestampEqualCredentialIssuance() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse(issuedKeyAddedOn = before, issuedKeyRevokedOn = after) },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = { GetCredentialRevocationTimeResponse() }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.KeyWasRevoked(after.toTimestampInfoModel(), after.toTimestampInfoModel()),
                            VerificationError.KeyRevokedTimestampEqualsIssuance(after.toTimestampInfoModel(), after.toTimestampInfoModel())
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain KeyWasRevoked & KeyRevokedTimestampEqualsIssuance"
                )
            }
        )
    }

    @Test
    fun testVerifyShouldReturnCredentialWasRevokedOnGivenCredentialHasRevocationTimestamp() {

        val signedCredential = setupDefaultSignedCredential()

        val (_, merkleInclusionProof) = rootAndProofFor(signedCredential)

        val nodeService = setupNodeService(
            getDidDocument = { setupGetDidDocumentResponse() },
            getBatchState = { setupGetBatchStateResponse(lastSyncedBlockTimestamp = after, issuedOn = after) },
            getCredentialRevocationTime = {
                GetCredentialRevocationTimeResponse(
                    revocationLedgerData = LedgerData(
                        timestampInfo = after
                    )
                )
            }
        )

        val verificationService = CredentialVerificationService(nodeService)

        runThenAssert(
            { verificationService.verify(signedCredential, merkleInclusionProof) },
            { verificationResult ->
                assertEquals(
                    VerificationResult(
                        listOf(
                            VerificationError.CredentialWasRevokedOn(after.toTimestampInfoModel()),
                        ),
                        after.blockTimestamp
                    ),
                    verificationResult,
                    "VerificationErrors should contain CredentialWasRevokedOn"
                )
            }
        )
    }

    private fun rootAndProofFor(signedCredential: PrismCredential): Pair<MerkleRoot, MerkleInclusionProof> {
        val (root, profs) = CredentialBatches.batch(listOf(signedCredential))
        return Pair(root, profs.first())
    }

    private fun setupDefaultSignedCredential(): PrismCredential {
        return setupDefaultJsonBasedCredential().sign(issuingKeyPair.privateKey)
    }

    private fun setupDefaultJsonBasedCredential() = JsonBasedCredential(
        CredentialContent(
            JsonObject(
                mapOf(
                    Pair(
                        "id",
                        JsonPrimitive(defaultDid.value)
                    ),
                    Pair("keyId", JsonPrimitive(issuingKeyId))
                )
            )
        )
    )

    private fun setupGetDidDocumentResponse(
        did1: DID? = defaultDid,
        issuedKeyAddedOn: TimestampInfo? = before,
        issuedKeyRevokedOn: TimestampInfo? = null
    ) = GetDidDocumentResponse(
        setupDidDataResponse(
            did = did1,
            issuingPublicKey = setupPublicKeyResponse(
                issuingKeyId,
                issuingKeyPair.publicKey,
                addedOn = issuedKeyAddedOn,
                revokedOn = issuedKeyRevokedOn
            )
        )
    )

    private fun setupGetDidDocumentResponse(
        did: DID? = defaultDid,
        issuingPublicKey: PublicKey? = setupPublicKeyResponse(issuingKeyId, issuingKeyPair.publicKey, addedOn = before),
    ) = GetDidDocumentResponse(
        setupDidDataResponse(
            did = did,
            issuingPublicKey = issuingPublicKey
        )
    )

    private fun setupDidDataResponse(
        did: DID?,
        masterPublicKey: PublicKey? = setupPublicKeyResponse(masterKeyId, masterKeyPair.publicKey, addedOn = before),
        issuingPublicKey: PublicKey?
    ) = DIDData(
        id = did?.value ?: "",
        publicKeys = listOfNotNull(
            masterPublicKey,
            issuingPublicKey
        )
    )

    private fun setupPublicKeyResponse(
        keyId: String,
        ecPublicKey: ECPublicKey?,
        addedOn: TimestampInfo?,
        revokedOn: TimestampInfo? = null
    ) = PublicKey(
        id = keyId,
        addedOn = addedOn,
        revokedOn = revokedOn,
        keyData = ecPublicKey?.toProto()?.let { PublicKey.KeyData.EcKeyData(it) }
    )

    private fun setupGetBatchStateResponse(
        lastSyncedBlockTimestamp: TimestampInfo?,
        issuedOn: TimestampInfo?,
        revokedOn: TimestampInfo? = null
    ): GetBatchStateResponse = setupGetBatchStateResponse(
        lastSyncedBlockTimestamp = lastSyncedBlockTimestamp,
        publicationLedgerData = LedgerData(timestampInfo = issuedOn),
        revokedOn = revokedOn
    )

    private fun setupGetBatchStateResponse(
        lastSyncedBlockTimestamp: TimestampInfo?,
        publicationLedgerData: LedgerData?,
        revokedOn: TimestampInfo? = null
    ): GetBatchStateResponse = GetBatchStateResponse(
        lastSyncedBlockTimestamp = lastSyncedBlockTimestamp?.blockTimestamp,
        publicationLedgerData = publicationLedgerData,
        revocationLedgerData = revokedOn?.let { LedgerData(timestampInfo = it) }
    )

    private fun setupNodeService(
        getDidDocument: (GetDidDocumentRequest) -> GetDidDocumentResponse = { TODO("Not yet implemented") },
        getBatchState: (GetBatchStateRequest) -> GetBatchStateResponse = { TODO("Not yet implemented") },
        getCredentialRevocationTime: (GetCredentialRevocationTimeRequest) -> GetCredentialRevocationTimeResponse =
            { TODO("Not yet implemented") }
    ): NodeServiceCoroutine = object : NodeServiceCoroutine {
        override suspend fun HealthCheck(req: HealthCheckRequest): HealthCheckResponse {
            TODO("Not yet implemented")
        }

        override suspend fun HealthCheckAuth(req: HealthCheckRequest, metadata: PrismMetadata): HealthCheckResponse {
            TODO("Not yet implemented")
        }

        override suspend fun GetDidDocument(req: GetDidDocumentRequest): GetDidDocumentResponse = getDidDocument(req)

        override suspend fun GetDidDocumentAuth(
            req: GetDidDocumentRequest,
            metadata: PrismMetadata
        ): GetDidDocumentResponse {
            TODO("Not yet implemented")
        }

        override suspend fun CreateDID(req: CreateDIDRequest): CreateDIDResponse {
            TODO("Not yet implemented")
        }

        override suspend fun CreateDIDAuth(req: CreateDIDRequest, metadata: PrismMetadata): CreateDIDResponse {
            TODO("Not yet implemented")
        }

        override suspend fun UpdateDID(req: UpdateDIDRequest): UpdateDIDResponse {
            TODO("Not yet implemented")
        }

        override suspend fun UpdateDIDAuth(req: UpdateDIDRequest, metadata: PrismMetadata): UpdateDIDResponse {
            TODO("Not yet implemented")
        }

        override suspend fun IssueCredentialBatch(req: IssueCredentialBatchRequest): IssueCredentialBatchResponse {
            TODO("Not yet implemented")
        }

        override suspend fun IssueCredentialBatchAuth(
            req: IssueCredentialBatchRequest,
            metadata: PrismMetadata
        ): IssueCredentialBatchResponse {
            TODO("Not yet implemented")
        }

        override suspend fun RevokeCredentials(req: RevokeCredentialsRequest): RevokeCredentialsResponse {
            TODO("Not yet implemented")
        }

        override suspend fun RevokeCredentialsAuth(
            req: RevokeCredentialsRequest,
            metadata: PrismMetadata
        ): RevokeCredentialsResponse {
            TODO("Not yet implemented")
        }

        override suspend fun GetTransactionStatus(req: GetTransactionStatusRequest): GetTransactionStatusResponse {
            TODO("Not yet implemented")
        }

        override suspend fun GetTransactionStatusAuth(
            req: GetTransactionStatusRequest,
            metadata: PrismMetadata
        ): GetTransactionStatusResponse {
            TODO("Not yet implemented")
        }

        override suspend fun GetNodeBuildInfo(req: GetNodeBuildInfoRequest): GetNodeBuildInfoResponse {
            TODO("Not yet implemented")
        }

        override suspend fun GetNodeBuildInfoAuth(
            req: GetNodeBuildInfoRequest,
            metadata: PrismMetadata
        ): GetNodeBuildInfoResponse {
            TODO("Not yet implemented")
        }

        override suspend fun GetBatchState(req: GetBatchStateRequest): GetBatchStateResponse = getBatchState(req)

        override suspend fun GetBatchStateAuth(
            req: GetBatchStateRequest,
            metadata: PrismMetadata
        ): GetBatchStateResponse {
            TODO("Not yet implemented")
        }

        override suspend fun GetCredentialRevocationTime(req: GetCredentialRevocationTimeRequest): GetCredentialRevocationTimeResponse =
            getCredentialRevocationTime(req)

        override suspend fun GetCredentialRevocationTimeAuth(
            req: GetCredentialRevocationTimeRequest,
            metadata: PrismMetadata
        ): GetCredentialRevocationTimeResponse {
            TODO("Not yet implemented")
        }

        override suspend fun GetOperationInfo(req: GetOperationInfoRequest): GetOperationInfoResponse {
            TODO("Not yet implemented")
        }

        override suspend fun GetOperationInfoAuth(
            req: GetOperationInfoRequest,
            metadata: PrismMetadata
        ): GetOperationInfoResponse {
            TODO("Not yet implemented")
        }

        override suspend fun PublishAsABlock(req: PublishAsABlockRequest): PublishAsABlockResponse {
            TODO("Not yet implemented")
        }

        override suspend fun PublishAsABlockAuth(
            req: PublishAsABlockRequest,
            metadata: PrismMetadata
        ): PublishAsABlockResponse {
            TODO("Not yet implemented")
        }
    }
}
