package io.iohk.cvp.data.local.remote

import com.google.protobuf.ByteString
import io.iohk.atala.crypto.japi.ECKeyPair
import io.iohk.cvp.viewmodel.dtos.ConnectionDataDto
import io.iohk.prism.protos.*

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