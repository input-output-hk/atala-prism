package io.iohk.atala.prism.app.data.local.remote

import com.google.protobuf.ByteString
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.app.viewmodel.dtos.ConnectionDataDto
import io.iohk.atala.prism.protos.*

interface ApiHelper {
    suspend fun addConnection(ecKeyPair: ECKeyPair,
                              token: String, nonce: String): AddConnectionFromTokenResponse

    suspend fun getAllMessages(ecKeyPair: ECKeyPair, lastMessageId: String?): GetMessagesPaginatedResponse

    suspend fun sendMultipleMessage(ecKeyPair: ECKeyPair, connectionId: String,
                                    messages: List<ByteString>)

    suspend fun getConnectionTokenInfo(token: String): GetConnectionTokenInfoResponse
    suspend fun sendMessageToMultipleConnections(connectionDataList: List<ConnectionDataDto>, credential: ByteString)
    suspend fun getConnection(ecKeyPair: ECKeyPair): GetConnectionsPaginatedResponse
}