package io.iohk.atala.prism.app.neo.data.remote

import com.google.protobuf.ByteString
import io.grpc.stub.MetadataUtils
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.app.utils.GrpcUtils
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.protos.AddConnectionFromTokenRequest
import io.iohk.atala.prism.protos.AddConnectionFromTokenResponse
import io.iohk.atala.prism.protos.ConnectorServiceGrpc
import io.iohk.atala.prism.protos.GetConnectionTokenInfoRequest
import io.iohk.atala.prism.protos.GetConnectionTokenInfoResponse
import io.iohk.atala.prism.protos.GetConnectionsPaginatedRequest
import io.iohk.atala.prism.protos.GetConnectionsPaginatedResponse
import io.iohk.atala.prism.protos.GetMessagesPaginatedRequest
import io.iohk.atala.prism.protos.GetMessagesPaginatedResponse
import io.iohk.atala.prism.protos.SendMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

class ConnectorRemoteDataSource(preferencesLocalDataSource: PreferencesLocalDataSourceInterface, private val sessionLocalDataSource: SessionLocalDataSourceInterface) : BaseRemoteDataSource(preferencesLocalDataSource) {

    private fun getChannel(ecKeyPair: ECKeyPair?, requestByteArray: ByteArray): ConnectorServiceGrpc.ConnectorServiceFutureStub {
        val mainChannel = getMainChannel()
        var stub = ConnectorServiceGrpc.newFutureStub(mainChannel)
        if (ecKeyPair != null) {
            stub = MetadataUtils.attachHeaders(stub, CryptoUtils.getMetadata(ecKeyPair, requestByteArray))
        }
        return stub
    }

    fun getConnections(ecKeyPair: ECKeyPair): GetConnectionsPaginatedResponse {
        val request = GetConnectionsPaginatedRequest.newBuilder().setLimit(QUERY_LENGTH_LIMIT).build()
        return getChannel(ecKeyPair, request.toByteArray()).getConnectionsPaginated(request).get()
    }

    fun getAllMessages(ecKeyPair: ECKeyPair, lastMessageId: String?): GetMessagesPaginatedResponse {
        val request = GetMessagesPaginatedRequest.newBuilder().setLimit(QUERY_LENGTH_LIMIT)
        if (lastMessageId != null)
            request.lastSeenMessageId = lastMessageId
        val requestBuild = request.build()
        return getChannel(ecKeyPair, requestBuild.toByteArray()).getMessagesPaginated(requestBuild).get()
    }

    /**
     * Sends a credential to multiple contacts
     *
     * @param contacts [List] of [Contact]
     * @param credential [Credential]
     */
    suspend fun sendCredentialToMultipleContacts(credential: Credential, contacts: List<Contact>) {
        return withContext(Dispatchers.IO) {
            val credentialByteArray = ByteString.copyFrom(credential.credentialEncoded.toByteArray())
            val phrases = sessionLocalDataSource.getSessionData()
            contacts.forEach { contact ->
                val keyPair = CryptoUtils.getKeyPairFromPath(contact.keyDerivationPath, phrases!!)
                val request = SendMessageRequest.newBuilder()
                    .setConnectionId(contact.connectionId).setMessage(credentialByteArray).build()
                val channel = getChannel(keyPair, request.toByteArray())
                channel.sendMessage(request).get()
            }
        }
    }

    suspend fun getConnectionTokenInfo(token: String): GetConnectionTokenInfoResponse = withContext(Dispatchers.IO) {
        val request = GetConnectionTokenInfoRequest
            .newBuilder()
            .setToken(token)
            .build()
        return@withContext getChannel(null, request.toByteArray()).getConnectionTokenInfo(request).get()
    }

    suspend fun addConnection(ecKeyPair: ECKeyPair, token: String, nonce: String): AddConnectionFromTokenResponse = withContext(Dispatchers.IO) {
        val request = AddConnectionFromTokenRequest.newBuilder()
            .setToken(token)
            .setPaymentNonce(nonce)
            .setHolderEncodedPublicKey(GrpcUtils.getPublicKeyEncoded(ecKeyPair))
            .build()
        return@withContext getChannel(ecKeyPair, request.toByteArray()).addConnectionFromToken(request).get()
    }

    fun sendMultipleMessage(ecKeyPair: ECKeyPair, connectionId: String, messages: List<ByteString>) {
        messages.forEach { byteString: ByteString ->
            val request = SendMessageRequest.newBuilder()
                .setConnectionId(connectionId).setMessage(byteString).build()
            getChannel(ecKeyPair, request.toByteArray()).sendMessage(request).get()
        }
    }

    suspend fun sendCredentialsToContact(contact: Contact, credentials: List<Credential>) = withContext(Dispatchers.IO) {
        val mnemonicList = sessionLocalDataSource.getSessionData()!!
        val keyPair = CryptoUtils.getKeyPairFromPath(contact.keyDerivationPath, mnemonicList)
        val messages: List<ByteString> = credentials.stream().map {
            it.credentialEncoded
        }.collect(Collectors.toList())
        sendMultipleMessage(keyPair, contact.connectionId, messages)
    }
}
