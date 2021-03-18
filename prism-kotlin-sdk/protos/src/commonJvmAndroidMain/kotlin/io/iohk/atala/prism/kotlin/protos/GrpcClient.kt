package io.iohk.atala.prism.kotlin.protos

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.MethodDescriptor
import io.grpc.kotlin.ClientCalls
import pbandk.Message
import pbandk.decodeFromStream
import pbandk.encodeToByteArray
import java.io.InputStream

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

    actual suspend fun <Req : Message, Resp : Message> call(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String
    ): Resp {
        val methodDescriptor = MethodDescriptor
            .newBuilder<Req, Resp>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, methodName))
            .setRequestMarshaller(MessageMarshaller(reqCompanion))
            .setResponseMarshaller(MessageMarshaller(respCompanion))
            .build()
        return ClientCalls.unaryRpc(channel, methodDescriptor, request)
    }
}
