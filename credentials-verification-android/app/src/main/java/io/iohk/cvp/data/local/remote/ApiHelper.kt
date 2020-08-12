package io.iohk.cvp.data.local.remote

import com.google.protobuf.ByteString
import io.iohk.cvp.viewmodel.dtos.ConnectionListable
import io.iohk.prism.protos.*

interface ApiHelper {
    suspend fun addConnection(token: String,
                      publicKey: ConnectorPublicKey, nonce: String): AddConnectionFromTokenResponse

    suspend fun getAllMessages(userId: String, lastMessageId: String?): GetMessagesPaginatedResponse

    suspend fun sendMultipleMessage(senderUserId: String, connectionId: String,
                                    messages: List<ByteString>)

    suspend fun getConnectionTokenInfo(token: String): GetConnectionTokenInfoResponse
    suspend fun sendMessageToMultipleConnections(senderUserList: MutableSet<ConnectionListable>, credential: ByteString)
}