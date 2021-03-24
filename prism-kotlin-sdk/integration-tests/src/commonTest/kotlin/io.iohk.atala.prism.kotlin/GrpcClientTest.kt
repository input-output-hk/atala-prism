package io.iohk.atala.prism.kotlin

import com.benasher44.uuid.bytes
import com.benasher44.uuid.uuid4
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.protos.*
import pbandk.ByteArr
import pbandk.encodeToByteArray
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class GrpcClientTest {
    @Test
    @Ignore
    fun testHealthCheckService() = runTest {
        val grpcClient = GrpcClient(
            GrpcServerOptions("http", "localhost", 50053),
            GrpcEnvoyOptions("http", "localhost", 10000)
        )
        val nodeService = NodeService.Client(grpcClient)
        val request = HealthCheckRequest()
        val response = nodeService.HealthCheck(request)
        assertEquals(HealthCheckResponse(), response)
    }

    @Test
    @Ignore
    fun testAuthenticatedService() = runTest {
        val grpcClient = GrpcClient(
            GrpcServerOptions("http", "localhost", 50051),
            GrpcEnvoyOptions("http", "localhost", 10000)
        )
        val connectorService = ConnectorService.Client(grpcClient)

        val keyPair = EC.generateKeyPair()
        val point = keyPair.publicKey.getCurvePoint()
        val ecKeyData =
            PublicKey.KeyData.EcKeyData(
                ECKeyData(
                    "secp256k1",
                    ByteArr(point.x.toByteArray()),
                    ByteArr(point.y.toByteArray())
                )
            )
        val publicKey = PublicKey("master0", KeyUsage.MASTER_KEY, keyData = ecKeyData)
        val createDidOperation = CreateDIDOperation(DIDData(publicKeys = listOf(publicKey)))
        val atalaOperation = AtalaOperation(AtalaOperation.Operation.CreateDid(createDidOperation))
        val signature = EC.sign(atalaOperation.encodeToByteArray().toList(), keyPair.privateKey)
        val request = RegisterDIDRequest(
            SignedAtalaOperation(
                "master0",
                ByteArr(signature.getEncoded().toByteArray()),
                atalaOperation
            ),
            RegisterDIDRequest.Role.ISSUER
        )
        val response = connectorService.RegisterDID(request)

        val getMessagesRequest = GetMessagesPaginatedRequest(limit = 10)
        val requestNonce = uuid4().bytes.toList()
        val didSignature = EC.sign(
            requestNonce + getMessagesRequest.encodeToByteArray().toList(),
            keyPair.privateKey
        )
        val metadata = PrismMetadata(
            did = response.did,
            didKeyId = "master0",
            didSignature = didSignature.getEncoded().toByteArray(),
            requestNonce = requestNonce.toByteArray()
        )
        val getMessagesResponse =
            connectorService.GetMessagesPaginatedAuth(getMessagesRequest, metadata)
        assertEquals(GetMessagesPaginatedResponse(), getMessagesResponse)
    }
}
