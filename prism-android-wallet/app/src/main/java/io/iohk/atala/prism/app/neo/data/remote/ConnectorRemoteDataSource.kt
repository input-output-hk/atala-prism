package io.iohk.atala.prism.app.neo.data.remote

import com.google.protobuf.ByteString
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import io.iohk.atala.kycbridge.protos.KycBridgeServiceGrpc
import io.iohk.atala.mirror.protos.MirrorServiceGrpc
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.EncodedCredential
import io.iohk.atala.prism.app.data.local.preferences.models.AcuantUserInfo
import io.iohk.atala.prism.app.neo.common.extensions.toByteArray
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.app.utils.GrpcUtils
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.protos.AcuantProcessFinished
import io.iohk.atala.prism.protos.AddConnectionFromTokenRequest
import io.iohk.atala.prism.protos.AddConnectionFromTokenResponse
import io.iohk.atala.prism.protos.AtalaMessage
import io.iohk.atala.prism.protos.CheckPayIdNameAvailabilityMessage
import io.iohk.atala.prism.protos.ConnectorServiceGrpc
import io.iohk.atala.prism.protos.GetConnectionTokenInfoRequest
import io.iohk.atala.prism.protos.GetConnectionTokenInfoResponse
import io.iohk.atala.prism.protos.GetConnectionsPaginatedRequest
import io.iohk.atala.prism.protos.GetConnectionsPaginatedResponse
import io.iohk.atala.prism.protos.GetMessageStreamRequest
import io.iohk.atala.prism.protos.GetMessageStreamResponse
import io.iohk.atala.prism.protos.GetMessagesPaginatedRequest
import io.iohk.atala.prism.protos.GetMessagesPaginatedResponse
import io.iohk.atala.prism.protos.KycBridgeMessage
import io.iohk.atala.prism.protos.MirrorMessage
import io.iohk.atala.prism.protos.PayIdNameRegistrationMessage
import io.iohk.atala.prism.protos.RegisterAddressMessage
import io.iohk.atala.prism.protos.RegisterWalletMessage
import io.iohk.atala.prism.protos.SendMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.stream.Collectors
import io.iohk.atala.kycbridge.protos.CreateAccountRequest as KycCreateAccountRequest
import io.iohk.atala.kycbridge.protos.CreateAccountResponse as KycCreateAccountResponse
import io.iohk.atala.mirror.protos.CreateAccountRequest as MirrorCreateAccountRequest
import io.iohk.atala.mirror.protos.CreateAccountResponse as MirrorCreateAccountResponse

class ConnectorRemoteDataSource(
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface,
    private val sessionLocalDataSource: SessionLocalDataSourceInterface
) : BaseRemoteDataSource(preferencesLocalDataSource) {

    private fun getChannel(
        ecKeyPair: ECKeyPair?,
        requestByteArray: ByteArray
    ): ConnectorServiceGrpc.ConnectorServiceFutureStub {
        val mainChannel = getMainChannel()
        var stub = ConnectorServiceGrpc.newFutureStub(mainChannel)
        if (ecKeyPair != null) {
            stub = MetadataUtils.attachHeaders(
                stub,
                CryptoUtils.getMetadata(ecKeyPair, requestByteArray)
            )
        }
        return stub
    }

    fun getConnections(ecKeyPair: ECKeyPair): GetConnectionsPaginatedResponse {
        val request =
            GetConnectionsPaginatedRequest.newBuilder().setLimit(QUERY_LENGTH_LIMIT).build()
        return getChannel(ecKeyPair, request.toByteArray()).getConnectionsPaginated(request).get()
    }

    fun getAllMessages(ecKeyPair: ECKeyPair, lastMessageId: String?): GetMessagesPaginatedResponse {
        val request = GetMessagesPaginatedRequest.newBuilder().setLimit(QUERY_LENGTH_LIMIT)
        if (lastMessageId != null)
            request.lastSeenMessageId = lastMessageId
        val requestBuild = request.build()
        return getChannel(ecKeyPair, requestBuild.toByteArray()).getMessagesPaginated(requestBuild)
            .get()
    }

    fun startMessagesStream(
        ecKeyPair: ECKeyPair,
        lastMessageId: String?,
        observer: StreamObserver<GetMessageStreamResponse>
    ) {
        var stub = ConnectorServiceGrpc.newStub(getMainChannel())
        val requestBuilder = GetMessageStreamRequest.newBuilder()
        lastMessageId?.let {
            requestBuilder.setLastSeenMessageId(it)
        }
        val request = requestBuilder.build()
        stub = MetadataUtils.attachHeaders(
            stub,
            CryptoUtils.getMetadata(ecKeyPair, request.toByteArray())
        )
        stub.getMessageStream(request, observer)
    }

    /**
     * Sends a credential to multiple contacts
     *
     * @param contacts [List] of [Contact]
     * @param encodedCredential [Credential]
     */
    suspend fun sendCredentialToMultipleContacts(
        encodedCredential: EncodedCredential,
        contacts: List<Contact>
    ) {
        return withContext(Dispatchers.IO) {
            val phrases = sessionLocalDataSource.getSessionData()
            contacts.forEach { contact ->
                val keyPair = CryptoUtils.getKeyPairFromPath(contact.keyDerivationPath, phrases!!)
                val request = SendMessageRequest.newBuilder()
                    .setConnectionId(contact.connectionId)
                    .setMessage(encodedCredential.credentialEncoded).build()
                val channel = getChannel(keyPair, request.toByteArray())
                channel.sendMessage(request).get()
            }
        }
    }

    suspend fun getConnectionTokenInfo(token: String): GetConnectionTokenInfoResponse =
        withContext(Dispatchers.IO) {
            val request = GetConnectionTokenInfoRequest
                .newBuilder()
                .setToken(token)
                .build()
            return@withContext getChannel(null, request.toByteArray()).getConnectionTokenInfo(
                request
            ).get()
        }

    suspend fun addConnection(ecKeyPair: ECKeyPair, token: String): AddConnectionFromTokenResponse =
        withContext(Dispatchers.IO) {
            val request = AddConnectionFromTokenRequest.newBuilder()
                .setToken(token)
                .setHolderEncodedPublicKey(GrpcUtils.getPublicKeyEncoded(ecKeyPair))
                .build()
            return@withContext getChannel(ecKeyPair, request.toByteArray()).addConnectionFromToken(
                request
            ).get()
        }

    fun sendMultipleMessage(
        ecKeyPair: ECKeyPair,
        connectionId: String,
        messages: List<ByteString>
    ) {
        messages.forEach { byteString: ByteString ->
            val request = SendMessageRequest.newBuilder()
                .setConnectionId(connectionId).setMessage(byteString).build()
            getChannel(ecKeyPair, request.toByteArray()).sendMessage(request).get()
        }
    }

    suspend fun sendCredentialsToContact(
        contact: Contact,
        encodedCredentials: List<EncodedCredential>
    ) = withContext(Dispatchers.IO) {
        val mnemonicList = sessionLocalDataSource.getSessionData()!!
        val keyPair = CryptoUtils.getKeyPairFromPath(contact.keyDerivationPath, mnemonicList)
        val messages: List<ByteString> = encodedCredentials.stream().map {
            it.credentialEncoded
        }.collect(Collectors.toList())
        sendMultipleMessage(keyPair, contact.connectionId, messages)
    }

    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    suspend fun sendAtalaMessage(atalaMessage: AtalaMessage, contact: Contact): String {
        val mnemonicList = sessionLocalDataSource.getSessionData()!!
        val keyPair = CryptoUtils.getKeyPairFromPath(contact.keyDerivationPath, mnemonicList)
        val request = SendMessageRequest.newBuilder()
            .setConnectionId(contact.connectionId).setMessage(atalaMessage.toByteString()).build()
        return getChannel(keyPair, request.toByteArray()).sendMessage(request).get().id
    }

    /*
    * MIRROR SERVICE
    * */

    suspend fun mirrorServiceCreateAccount(): MirrorCreateAccountResponse =
        withContext(Dispatchers.IO) {
            val stub = MirrorServiceGrpc.newFutureStub(getMirrorServiceChannel())
            val request = MirrorCreateAccountRequest.newBuilder().build()
            stub.createAccount(request).get()
        }

    @Throws(ExecutionException::class, InterruptedException::class)
    suspend fun sendMirrorMessage(mirrorMessage: MirrorMessage, mirrorContact: Contact): String {
        val atalaMessage = AtalaMessage.newBuilder()
            .setMirrorMessage(mirrorMessage)
            .build()
        return sendAtalaMessage(atalaMessage, mirrorContact)
    }

    /**
     * Send a [CheckPayIdNameAvailabilityMessage] message to a Mirror Connection
     * @param payIdName name to verify
     * @param mirrorContact a mirror connection contact
     * @return id of the sent message
     * */
    @Throws(ExecutionException::class, InterruptedException::class)
    suspend fun sendCheckPayIdNameAvailabilityMessage(
        payIdName: String,
        mirrorContact: Contact
    ): String {
        val message =
            CheckPayIdNameAvailabilityMessage.newBuilder().setNameToCheck(payIdName).build()
        val mirrorMessage =
            MirrorMessage.newBuilder().setCheckPayIdNameAvailabilityMessage(message).build()
        return sendMirrorMessage(mirrorMessage, mirrorContact)
    }

    /**
     * Send a [PayIdNameRegistrationMessage] message to a Mirror Connection
     * @param payIdName name to register
     * @param mirrorContact a mirror connection contact
     * @return id of the sent message
     * */
    @Throws(ExecutionException::class, InterruptedException::class)
    suspend fun sendPayIdNameRegistrationMessage(
        payIdName: String,
        mirrorContact: Contact,
    ): String {
        val message = PayIdNameRegistrationMessage.newBuilder().setName(payIdName).build()
        val mirrorMessage =
            MirrorMessage.newBuilder().setPayIdNameRegistrationMessage(message).build()
        return sendMirrorMessage(mirrorMessage, mirrorContact)
    }

    /**
     * Send a [RegisterAddressMessage] message to a Mirror Connection
     * @param cardanoAddress address to register
     * @param mirrorContact a mirror connection contact
     * @return id of the sent message
     * */
    @Throws(ExecutionException::class, InterruptedException::class)
    suspend fun sendRegisterAddressMessage(cardanoAddress: String, mirrorContact: Contact): String {
        val message = RegisterAddressMessage.newBuilder().setCardanoAddress(cardanoAddress).build()
        val mirrorMessage = MirrorMessage.newBuilder().setRegisterAddressMessage(message).build()
        return sendMirrorMessage(mirrorMessage, mirrorContact)
    }

    suspend fun sendKycData(acuantUserInfo: AcuantUserInfo, kycContact: Contact) {
        val selfieData = ByteString.copyFrom(acuantUserInfo.selfieImage!!.toByteArray())
        val atalaMessage = AtalaMessage.newBuilder()
            .setKycBridgeMessage(
                KycBridgeMessage
                    .newBuilder()
                    .setAcuantProcessFinished(
                        AcuantProcessFinished
                            .newBuilder()
                            .setDocumentInstanceId(acuantUserInfo.instanceId)
                            .setSelfieImage(selfieData)
                            .build()
                    ).build()
            ).build()
        val mnemonicList = sessionLocalDataSource.getSessionData()!!
        val keyPair = CryptoUtils.getKeyPairFromPath(kycContact.keyDerivationPath, mnemonicList)
        val request = SendMessageRequest.newBuilder()
            .setConnectionId(kycContact.connectionId).setMessage(atalaMessage.toByteString()).build()
        getChannel(keyPair, request.toByteArray()).sendMessage(request).get()
    }

    /**
     * Send a [RegisterWalletMessage] message to a Mirror Connection
     * @param publicKey as acct_xvk string
     * @param mirrorContact a mirror connection contact
     * @return id of the sent message
     * */
    @Throws(ExecutionException::class, InterruptedException::class)
    suspend fun sendRegisterWalletMessage(publicKey: String, mirrorContact: Contact): String {
        val message = RegisterWalletMessage.newBuilder().setExtendedPublicKey(publicKey).build()
        val mirrorMessage = MirrorMessage.newBuilder().setRegisterWalletMessage(message).build()
        return sendMirrorMessage(mirrorMessage, mirrorContact)
    }

    /**
     * KycBridgeService/CreateAccount
     * */
    suspend fun kycBridgeCreateAccount(): KycCreateAccountResponse = withContext(Dispatchers.IO) {
        val stub = KycBridgeServiceGrpc.newFutureStub(getKycBridgeChannel())
        val request = KycCreateAccountRequest.newBuilder().build()
        stub.createAccount(request).get()
    }
}
