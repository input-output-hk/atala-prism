package io.iohk.atala.prism.kotlin.protos

import grpc_web.*
import kotlinx.coroutines.await
import pbandk.Message
import pbandk.decodeFromByteArray
import pbandk.encodeToByteArray
import kotlin.js.json

actual class GrpcClient actual constructor(
    serverOptions: GrpcServerOptions,
    private val envoyOptions: GrpcEnvoyOptions
) {
    actual suspend fun <Req : Message, Resp : Message> call(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String
    ): Resp {
        val client = GrpcWebClientBase(object : GrpcWebClientBaseOptions {})
        val methodDescriptor: MethodDescriptor<Req, Resp> = MethodDescriptor(
            "/$serviceName/$methodName",
            "unary",
            { reqCompanion.asDynamic().defaultInstance },
            { respCompanion.asDynamic().defaultInstance },
            { req: Req -> req.encodeToByteArray() },
            { b: ByteArray -> respCompanion.decodeFromByteArray(b) }
        )

        return client.thenableCall(
            "${envoyOptions.protocol}://${envoyOptions.host}:${envoyOptions.port}/$serviceName/$methodName",
            request,
            json().unsafeCast<Metadata>(),
            methodDescriptor
        ).await()
    }
}
