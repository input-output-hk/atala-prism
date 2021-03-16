package io.iohk.atala.prism.kotlin.protos

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
}
