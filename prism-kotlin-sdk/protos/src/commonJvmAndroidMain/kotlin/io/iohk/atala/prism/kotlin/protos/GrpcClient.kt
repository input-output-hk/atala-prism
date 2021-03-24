package io.iohk.atala.prism.kotlin.protos

import io.grpc.*
import io.grpc.kotlin.ClientCalls
import io.grpc.stub.MetadataUtils
import pbandk.Message
import pbandk.decodeFromStream
import pbandk.encodeToByteArray
import java.io.InputStream
import java.util.*

actual class GrpcClient actual constructor(
    serverOptions: GrpcServerOptions,
    envoyOptions: GrpcEnvoyOptions
) {
    private val channel: ManagedChannel = ManagedChannelBuilder.forAddress(
        serverOptions.host,
        serverOptions.port
    ).usePlaintext().build()

    class MessageMarshaller<T : Message>(private val companion: Message.Companion<T>) :
        MethodDescriptor.Marshaller<T> {
        override fun stream(value: T): InputStream =
            value.encodeToByteArray().inputStream()

        override fun parse(stream: InputStream?): T =
            companion.decodeFromStream(stream!!)
    }

    private fun <Req : Message, Resp : Message> methodDescriptor(
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String
    ): MethodDescriptor<Req, Resp> {
        return MethodDescriptor
            .newBuilder<Req, Resp>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
            .setRequestMarshaller(MessageMarshaller(reqCompanion))
            .setResponseMarshaller(MessageMarshaller(respCompanion))
            .build()
    }

    actual suspend fun <Req : Message, Resp : Message> call(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String
    ): Resp {
        val methodDescriptor =
            methodDescriptor(reqCompanion, respCompanion, serviceName, methodName)
        return ClientCalls.unaryRpc(channel, methodDescriptor, request)
    }

    actual suspend fun <Req : Message, Resp : Message> callAuth(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String,
        prismMetadata: PrismMetadata
    ): Resp {
        val metadata = Metadata()
        metadata.put(DID_HEADER, prismMetadata.did)
        metadata.put(DID_KEY_ID_HEADER, prismMetadata.didKeyId)
        metadata.put(
            DID_SIGNATURE_HEADER,
            Base64.getUrlEncoder().encode(prismMetadata.didSignature).decodeToString()
        )
        metadata.put(
            REQUEST_NONCE_HEADER,
            Base64.getUrlEncoder().encode(prismMetadata.requestNonce).decodeToString()
        )
        val interceptor = MetadataUtils.newAttachHeadersInterceptor(metadata)
        val authChannel = ClientInterceptors.intercept(channel, interceptor)
        val methodDescriptor =
            methodDescriptor(reqCompanion, respCompanion, serviceName, methodName)
        return ClientCalls.unaryRpc(authChannel, methodDescriptor, request)
    }

    companion object {
        val DID_HEADER: Metadata.Key<String> =
            Metadata.Key.of(DID, Metadata.ASCII_STRING_MARSHALLER)
        val DID_KEY_ID_HEADER: Metadata.Key<String> =
            Metadata.Key.of(DID_KEY_ID, Metadata.ASCII_STRING_MARSHALLER)
        val DID_SIGNATURE_HEADER: Metadata.Key<String> =
            Metadata.Key.of(DID_SIGNATURE, Metadata.ASCII_STRING_MARSHALLER)
        val REQUEST_NONCE_HEADER: Metadata.Key<String> =
            Metadata.Key.of(REQUEST_NONCE, Metadata.ASCII_STRING_MARSHALLER)
    }
}
