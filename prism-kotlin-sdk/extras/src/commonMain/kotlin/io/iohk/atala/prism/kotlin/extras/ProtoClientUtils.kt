package io.iohk.atala.prism.kotlin.extras

import io.iohk.atala.prism.kotlin.protos.*

object ProtoClientUtils {
    fun nodeClient(host: String, port: Int): NodeServiceCoroutine.Client {
        val grpcClient = GrpcClient(
            GrpcServerOptions("http", host, port),
            GrpcEnvoyOptions("http", host, port)
        )
        return NodeServiceCoroutine.Client(grpcClient)
    }

    fun connectorClient(host: String, port: Int): ConnectorServiceCoroutine.Client {
        val grpcClient = GrpcClient(
            GrpcServerOptions("http", host, port),
            GrpcEnvoyOptions("http", host, port)
        )
        return ConnectorServiceCoroutine.Client(grpcClient)
    }
}
