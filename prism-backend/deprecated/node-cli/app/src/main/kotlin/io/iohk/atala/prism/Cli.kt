package io.iohk.atala.prism

import io.iohk.atala.prism.api.KeyGenerator
import io.iohk.atala.prism.api.models.AtalaOperationId
import io.iohk.atala.prism.api.models.AtalaOperationStatus
import io.iohk.atala.prism.api.node.NodeAuthApi
import io.iohk.atala.prism.api.node.NodePayloadGenerator
import io.iohk.atala.prism.api.node.NodePublicApi
import io.iohk.atala.prism.common.PrismSdkInternal
import io.iohk.atala.prism.crypto.derivation.KeyDerivation
import io.iohk.atala.prism.crypto.derivation.MnemonicCode
import io.iohk.atala.prism.crypto.keys.ECKeyPair
import io.iohk.atala.prism.identity.LongFormPrismDid
import io.iohk.atala.prism.identity.PrismDid
import io.iohk.atala.prism.identity.PrismKeyType
import io.iohk.atala.prism.protos.GetNodeBuildInfoRequest
import io.iohk.atala.prism.protos.GetOperationInfoRequest
import io.iohk.atala.prism.protos.NodeServiceCoroutine
import kotlinx.coroutines.runBlocking
import pbandk.ByteArr

// Waits until an operation is confirmed by the Cardano network.
// NOTE: Confirmation doesn't necessarily mean that operation was applied.
// For example, it could be rejected because of an incorrect signature or other reasons.
@OptIn(PrismSdkInternal::class)
fun waitUntilConfirmed(
    asyncClient: NodeServiceCoroutine,
    nodePublicApi: NodePublicApi,
    operationId: AtalaOperationId
) {
    fun transactionId(oid: AtalaOperationId): String {
        val response = runBlocking {
            asyncClient.GetOperationInfo(GetOperationInfoRequest(ByteArr(oid.value())))
        }
        return response.transactionId
    }

    var tid = ""
    var status = runBlocking {
        nodePublicApi.getOperationStatus(operationId)
    }
    while (status != AtalaOperationStatus.CONFIRMED_AND_APPLIED &&
        status != AtalaOperationStatus.CONFIRMED_AND_REJECTED
    ) {
        println("Current operation status: ${AtalaOperationStatus.asString(status)}")
        if (tid.isNullOrEmpty()) {
            tid = transactionId(operationId)
            if (!tid.isNullOrEmpty()) {
                println("Transaction id: $tid")
                println("Track the transaction in:\n- https://explorer.cardano-testnet.iohkdev.io/en/transaction?id=$tid")
            }
        }

        Thread.sleep(30000)
        status = runBlocking {
            nodePublicApi.getOperationStatus(operationId)
        }
    }
}

// Creates a list of potentially useful keys out of a mnemonic code
fun prepareKeysFromMnemonic(mnemonic: MnemonicCode, pass: String): Map<String, ECKeyPair> {
    val seed = KeyDerivation.binarySeed(mnemonic, pass)
    val issuerMasterKeyPair = KeyGenerator.deriveKeyFromFullPath(seed, 0, PrismKeyType.MASTER_KEY, 0)
    val issuerIssuingKeyPair = KeyGenerator.deriveKeyFromFullPath(seed, 0, PrismKeyType.ISSUING_KEY, 0)
    val issuerRevocationKeyPair = KeyGenerator.deriveKeyFromFullPath(seed, 0, PrismKeyType.REVOCATION_KEY, 0)
    return mapOf(
        Pair(PrismDid.DEFAULT_MASTER_KEY_ID, issuerMasterKeyPair),
        Pair(PrismDid.DEFAULT_ISSUING_KEY_ID, issuerIssuingKeyPair),
        Pair(PrismDid.DEFAULT_REVOCATION_KEY_ID, issuerRevocationKeyPair)
    )
}

@OptIn(PrismSdkInternal::class)
fun publishDid(
    asyncClient: NodeServiceCoroutine,
    nodeAuthApi: NodeAuthApi,
    unpublishedDid: LongFormPrismDid,
    keys: Map<String, ECKeyPair>
) {
    val did = unpublishedDid.asCanonical()

    var nodePayloadGenerator = NodePayloadGenerator(
        unpublishedDid,
        mapOf(PrismDid.DEFAULT_MASTER_KEY_ID to keys[PrismDid.DEFAULT_MASTER_KEY_ID]?.privateKey!!)
    )

    // creation of CreateDID operation
    val createDidInfo = nodePayloadGenerator.createDid()

    // sending CreateDID operation to the ledger
    val createDidOperationId = runBlocking {
        nodeAuthApi.createDid(
            createDidInfo.payload,
            unpublishedDid,
            PrismDid.DEFAULT_MASTER_KEY_ID
        )
    }

    println(
        """
        - Sent a request to create a new DID to PRISM Node.
        - The transaction can take up to 10 minutes to be confirmed by the Cardano network.
        - Operation identifier: ${createDidOperationId.hexValue()}
        """.trimIndent()
    )
    println()

    // Wait until Cardano network confirms the DID creation
    waitUntilConfirmed(asyncClient, nodeAuthApi, createDidOperationId)

    println(
        """
        - DID with id $did is created
        - Operation hash: ${createDidInfo.operationHash}
        """.trimIndent()
    )
}

@OptIn(PrismSdkInternal::class)
fun main(args: Array<String>) {
    parseArgs(
        args,
        object : CommandsHandlers {
            override fun healthCheck(nodePublicApi: NodePublicApi) {
                runBlocking { nodePublicApi.healthCheck() }
                println("Node is up!")
            }

            override fun getBuildInfo(node: NodeServiceCoroutine) {
                val response = runBlocking {
                    node.GetNodeBuildInfo(GetNodeBuildInfoRequest())
                }
                println(response)
            }

            override fun createDid(asyncClient: NodeServiceCoroutine, nodeAuthApi: NodeAuthApi) {
                println("Generates and registers a DID")
                val keys = prepareKeysFromMnemonic(KeyDerivation.randomMnemonicCode(), "passphrase")
                val unpublishedDid =
                    PrismDid.buildLongFormFromMasterPublicKey(keys[PrismDid.DEFAULT_MASTER_KEY_ID]?.publicKey!!)
                publishDid(asyncClient, nodeAuthApi, unpublishedDid, keys)
            }

            override fun resolveDid(nodePublicApi: NodePublicApi, did: PrismDid) {
                val result = runBlocking { nodePublicApi.getDidDocument(did) }
                println("- Prism DID: ${result.did}")
                println("- DID's public keys: ${result.publicKeys.joinToString(", ") { it.toString() }}")
            }
        }
    )
}
