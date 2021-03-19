package io.iohk.atala.prism.kotlin.identity

import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.protos.*
import kotlinx.coroutines.runBlocking
import pbandk.ByteArr
import pbandk.encodeToByteArray
import java.util.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class GrpcAuthenticationTest {
    @Test
    @Ignore
    fun testAuthenticatedService() = runBlocking {
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
        val requestNonce = UUID.randomUUID().toString().encodeToByteArray()
        val didSignature = EC.sign(requestNonce.toList() + getMessagesRequest.encodeToByteArray().toList(), keyPair.privateKey)
        val metadata = PrismMetadata(response.did, "master0", didSignature.getEncoded().toByteArray(), requestNonce)
        val getMessagesResponse = connectorService.GetMessagesPaginatedAuth(getMessagesRequest, metadata)
        assertEquals(GetMessagesPaginatedResponse(), getMessagesResponse)
    }
}
