package io.iohk.atala.prism.kotlin.protos

import pbandk.Message

actual class GrpcClient actual constructor(
    serverOptions: GrpcServerOptions,
    envoyOptions: GrpcEnvoyOptions
) {
    actual suspend fun <Req : Message, Resp : Message> call(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String
    ): Resp {
        TODO("iOS GRPC client is not supported yet")
    }
}
