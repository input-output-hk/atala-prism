package io.iohk.cvp.data.local.remote

import android.content.Context
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.iohk.cvp.BuildConfig
import io.iohk.cvp.core.CvpApplication
import io.iohk.cvp.viewmodel.dtos.ConnectionListable
import io.iohk.cvp.views.Preferences
import io.iohk.prism.protos.*
import javax.inject.Inject

class AppApiHelper @Inject constructor(cvpApplication: CvpApplication) : ApiHelper {

    companion object {
        private val HEADER_USER_ID_KEY = Metadata.Key.of("userId", Metadata.ASCII_STRING_MARSHALLER)
        private const val QUERY_LENGTH = 100
    }

    private var context: Context? = null
    private lateinit var origChannel: ManagedChannel

    init {
        context = cvpApplication.applicationContext
        initChannel()
    }

    private fun initChannel() {
        val prefs = Preferences(context)
        val ip = prefs.getString(Preferences.BACKEND_IP)
        val port = prefs.getInt(Preferences.BACKEND_PORT)
        origChannel = ManagedChannelBuilder
                .forAddress(if (ip == "") BuildConfig.API_BASE_URL else ip,
                        if (port == 0) BuildConfig.API_PORT else port)
                .usePlaintext()
                .build()
    }

    private fun getChannel(userId: String?): ConnectorServiceGrpc.ConnectorServiceBlockingStub {
        var stub = ConnectorServiceGrpc.newBlockingStub(origChannel)
        if(userId != null) {
            val m = Metadata()
            m.put(HEADER_USER_ID_KEY, userId)
            stub = MetadataUtils.attachHeaders(stub, m)
        }
        return stub
    }

    override suspend fun addConnection(token: String, publicKey: ConnectorPublicKey, nonce: String): AddConnectionFromTokenResponse {
        val request = AddConnectionFromTokenRequest.newBuilder()
                .setToken(token)
                .setPaymentNonce(nonce)
                .setHolderPublicKey(publicKey).build()
        return getChannel(null).addConnectionFromToken(request)
    }

    override suspend fun getAllMessages(userId: String, lastMessageId: String?): GetMessagesPaginatedResponse {
        val request = GetMessagesPaginatedRequest.newBuilder().setLimit(QUERY_LENGTH)
        if(lastMessageId != null)
            request.lastSeenMessageId = lastMessageId
        return getChannel(userId).getMessagesPaginated(request.build())
    }

    override suspend fun sendMultipleMessage(senderUserId: String, connectionId: String, messages: List<ByteString>) {
        val channel = getChannel(senderUserId)
        messages.forEach {
            byteString: ByteString ->
            val request = SendMessageRequest.newBuilder()
                    .setConnectionId(connectionId).setMessage(byteString)
            channel.sendMessage(request.build())
        }
    }

    override suspend fun getConnectionTokenInfo(token : String): GetConnectionTokenInfoResponse {
        val request = GetConnectionTokenInfoRequest.newBuilder()
                .setToken(token)
        return getChannel(null).getConnectionTokenInfo(request.build())
    }

    override suspend fun sendMessageToMultipleConnections(senderUserList: MutableSet<ConnectionListable>, credential: ByteString) {
        senderUserList.forEach {
            connectionListable: ConnectionListable ->
            run {
                val channel = getChannel(connectionListable.userIdValue)
                val request = SendMessageRequest.newBuilder()
                        .setConnectionId(connectionListable.connectionIdValue).setMessage(credential)
                channel.sendMessage(request.build())
            }
        }
    }

}