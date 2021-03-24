package io.iohk.atala.prism.kotlin.protos

import grpc_web.*
import io.iohk.atala.prism.kotlin.protos.util.Base64Utils
import kotlinx.coroutines.await
import pbandk.Message
import pbandk.decodeFromByteArray
import pbandk.encodeToByteArray
import kotlin.js.json

actual class GrpcClient actual constructor(
    serverOptions: GrpcServerOptions,
    private val envoyOptions: GrpcEnvoyOptions
) {
    private fun <Req : Message, Resp : Message> methodDescriptor(
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String
    ): MethodDescriptor<Req, Resp> {
        return MethodDescriptor(
            "/$serviceName/$methodName",
            "unary",
            { reqCompanion.asDynamic().defaultInstance },
            { respCompanion.asDynamic().defaultInstance },
            { req: Req -> req.encodeToByteArray() },
            { b: ByteArray -> respCompanion.decodeFromByteArray(b) }
        )
    }

    actual suspend fun <Req : Message, Resp : Message> call(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String
    ): Resp {
        val client = GrpcWebClientBase(object : GrpcWebClientBaseOptions {})
        val methodDescriptor =
            methodDescriptor(reqCompanion, respCompanion, serviceName, methodName)

        return client.thenableCall(
            "${envoyOptions.protocol}://${envoyOptions.host}:${envoyOptions.port}/$serviceName/$methodName",
            request,
            json().unsafeCast<Metadata>(),
            methodDescriptor
        ).await()
    }

    actual suspend fun <Req : Message, Resp : Message> callAuth(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String,
        prismMetadata: PrismMetadata
    ): Resp {
        val client = GrpcWebClientBase(object : GrpcWebClientBaseOptions {})
        val methodDescriptor =
            methodDescriptor(reqCompanion, respCompanion, serviceName, methodName)

        return client.thenableCall(
            "${envoyOptions.protocol}://${envoyOptions.host}:${envoyOptions.port}/$serviceName/$methodName",
            request,
            json(
                DID to prismMetadata.did,
                DID_KEY_ID to prismMetadata.didKeyId,
                DID_SIGNATURE to Base64Utils.encode(prismMetadata.didSignature.toList()),
                REQUEST_NONCE to Base64Utils.encode(prismMetadata.requestNonce.toList())
            ).unsafeCast<Metadata>(),
            methodDescriptor
        ).await()
    }
}
