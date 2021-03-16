package io.iohk.atala.prism.kotlin.protos

import pbandk.Message

data class GrpcServerOptions(val protocol: String, val host: String, val port: Int)
data class GrpcEnvoyOptions(val protocol: String, val host: String, val port: Int)

expect class GrpcClient(serverOptions: GrpcServerOptions, envoyOptions: GrpcEnvoyOptions) {
    suspend fun <Req : Message, Resp : Message> call(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String
    ): Resp
}
