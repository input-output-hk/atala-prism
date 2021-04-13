package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.protos.*

object ProtoClientUtils {
    fun nodeClient(host: String, port: Int): NodeService.Client {
        val grpcClient = GrpcClient(
            GrpcServerOptions("http", host, port),
            GrpcEnvoyOptions("http", host, port)
        )
        return NodeService.Client(grpcClient)
    }

    fun connectorClient(host: String, port: Int): ConnectorService.Client {
        val grpcClient = GrpcClient(
            GrpcServerOptions("http", host, port),
            GrpcEnvoyOptions("http", host, port)
        )
        return ConnectorService.Client(grpcClient)
    }
}
