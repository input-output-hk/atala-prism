package io.iohk.atala.prism.app.data.local.remote

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.MetadataUtils
import io.iohk.atala.prism.crypto.japi.ECKeyPair
import io.iohk.cvp.BuildConfig
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.app.utils.GrpcUtils
import io.iohk.atala.prism.app.viewmodel.dtos.ConnectionDataDto
import io.iohk.atala.prism.app.views.Preferences
import io.iohk.atala.prism.protos.*
import javax.inject.Inject

class AppApiHelper @Inject constructor(private val prefs: Preferences) : ApiHelper {

    companion object {
        private const val QUERY_LENGTH = 100
    }

    private lateinit var origChannel: ManagedChannel

    init {
        initChannel()
    }

    private fun initChannel() {
        val ip = prefs.getString(Preferences.BACKEND_IP)
        val port = prefs.getInt(Preferences.BACKEND_PORT)
        origChannel = ManagedChannelBuilder
                .forAddress(if (ip == "") BuildConfig.API_BASE_URL else ip,
                        if (port == 0) BuildConfig.API_PORT else port)
                .usePlaintext()
                .build()
    }

    private fun getChannel(ecKeyPair: ECKeyPair?, requestByteArray: ByteArray): ConnectorServiceGrpc.ConnectorServiceBlockingStub {
        var stub = ConnectorServiceGrpc.newBlockingStub(origChannel)
        if (ecKeyPair != null) {
            stub = MetadataUtils.attachHeaders(stub, CryptoUtils.getMetadata(ecKeyPair, requestByteArray))
        }
        return stub
    }

    override suspend fun addConnection(ecKeyPair: ECKeyPair, token: String, nonce: String): AddConnectionFromTokenResponse {

        val request = AddConnectionFromTokenRequest.newBuilder()
                .setToken(token)
                .setPaymentNonce(nonce)
                .setHolderEncodedPublicKey(GrpcUtils.getPublicKeyEncoded(ecKeyPair))
                .build()
        return getChannel(ecKeyPair, request.toByteArray()).addConnectionFromToken(request)
    }

    override suspend fun getAllMessages(ecKeyPair: ECKeyPair, lastMessageId: String?): GetMessagesPaginatedResponse {
        val request = GetMessagesPaginatedRequest.newBuilder().setLimit(QUERY_LENGTH)
        if (lastMessageId != null)
            request.lastSeenMessageId = lastMessageId
        val requestBuild = request.build()
        return getChannel(ecKeyPair, requestBuild.toByteArray()).getMessagesPaginated(requestBuild)
    }

    override suspend fun sendMultipleMessage(ecKeyPair: ECKeyPair, connectionId: String, messages: List<ByteString>) {
        messages.forEach { byteString: ByteString ->
            val request = SendMessageRequest.newBuilder()
                    .setConnectionId(connectionId).setMessage(byteString).build()
            getChannel(ecKeyPair, request.toByteArray()).sendMessage(request)
        }
    }

    override suspend fun getConnectionTokenInfo(token: String): GetConnectionTokenInfoResponse {
        val request = GetConnectionTokenInfoRequest.newBuilder()
                .setToken(token)
        return getChannel(null, request.build().toByteArray()).getConnectionTokenInfo(request.build())
    }

    override suspend fun sendMessageToMultipleConnections(connectionDataList: List<ConnectionDataDto>, credential: ByteString) {
        connectionDataList.forEach { connectionDataDto: ConnectionDataDto ->
            run {
                val request = SendMessageRequest.newBuilder()
                        .setConnectionId(connectionDataDto.connectionId).setMessage(credential).build()
                val channel = getChannel(connectionDataDto.ecKeyPairFromPath, request.toByteArray())
                channel.sendMessage(request)
            }
        }
    }

    override suspend fun getConnection(ecKeyPair: ECKeyPair): GetConnectionsPaginatedResponse {
        val request = GetConnectionsPaginatedRequest.newBuilder().setLimit(QUERY_LENGTH).build()
        return getChannel(ecKeyPair, request.toByteArray()).getConnectionsPaginated(request)
    }

}