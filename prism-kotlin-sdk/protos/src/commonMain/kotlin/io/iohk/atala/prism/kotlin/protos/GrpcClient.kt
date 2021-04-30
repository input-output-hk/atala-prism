package io.iohk.atala.prism.kotlin.protos

import pbandk.Message
import kotlin.js.JsExport

@JsExport
data class GrpcServerOptions(val protocol: String, val host: String, val port: Int)
@JsExport
data class GrpcEnvoyOptions(val protocol: String, val host: String, val port: Int)

@JsExport
class PrismMetadata(
    val did: String,
    val didKeyId: String,
    val didSignature: ByteArray,
    val requestNonce: ByteArray
)

const val DID = "did"
const val DID_KEY_ID = "didKeyId"
const val DID_SIGNATURE = "didSignature"
const val REQUEST_NONCE = "requestNonce"

expect class GrpcClient(serverOptions: GrpcServerOptions, envoyOptions: GrpcEnvoyOptions) {
    suspend fun <Req : Message, Resp : Message> call(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String
    ): Resp

    suspend fun <Req : Message, Resp : Message> callAuth(
        request: Req,
        reqCompanion: Message.Companion<Req>,
        respCompanion: Message.Companion<Resp>,
        serviceName: String,
        methodName: String,
        prismMetadata: PrismMetadata
    ): Resp
}
