package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.iohk.atala.prism.app.data.local.db.mappers.CredentialMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials
import io.iohk.atala.prism.app.neo.common.extensions.toMilliseconds
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SyncLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorGRPCStream
import io.iohk.atala.prism.app.neo.data.remote.ConnectorGRPCStreamsManager
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.protos.AtalaMessage
import io.iohk.atala.prism.protos.ReceivedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncRepository(
    private val syncLocalDataSource: SyncLocalDataSourceInterface,
    private val remoteDataSource: ConnectorRemoteDataSource,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) :
    BaseRepository(
        sessionLocalDataSource,
        preferencesLocalDataSource
    ) {

    private val contactsData = syncLocalDataSource.allContacts()

    // this observer must notify the ConnectorGRPCStreamsManager every change in the list of contacts (connections)
    private val contactsObserver = Observer<List<Contact>> { contacts ->
        CoroutineScope(Dispatchers.Default).launch {
            sessionData()?.let { mnemonicList ->
                connectorGRPCStreamsManager.handleConnections(contacts, mnemonicList)
            }
        }
    }

    // gets the ConnectorGRPCStreamsManager instance and is assigned a listener
    private val connectorGRPCStreamsManager = ConnectorGRPCStreamsManager.getInstance(
        remoteDataSource,
        object : ConnectorGRPCStream.OnMessageListener {
            override fun newMessage(receivedMessage: ReceivedMessage, connectionId: String) {
                // here is listened each new message coming from the multiple streams of gRPC data
                handleNewMessage(receivedMessage, connectionId)
            }
        }
    )

    /**
     * Start gRPC data streams
     * */
    fun startConnectionsStreams() {
        /** this really begins to observe the changes in the contact list (connections)
         * and this observer will notify the [ConnectorGRPCStreamsManager] so that it
         * manages the data streams based on the existing contacts.
         */
        contactsData.observeForever(contactsObserver)
    }

    /**
     * Stop gRPC data streams
     * */
    fun stopConnectionsStreams() {
        // contacts observer is removed
        contactsData.removeObserver(contactsObserver)
        /** All data streams are stopped and removed from the [ConnectorGRPCStreamsManager] */
        connectorGRPCStreamsManager.stopAndRemoveAllDataStreams()
    }

    /**
     * Handles each of the new messages from multiple data streams
     * */
    private fun handleNewMessage(receivedMessage: ReceivedMessage, connectionId: String) {
        CoroutineScope(Dispatchers.Default).launch {
            syncLocalDataSource.getContactByConnectionId(connectionId)?.let { contact ->
                val atalaMessage = AtalaMessage.parseFrom(receivedMessage.message)
                if (CredentialMapper.isACredentialMessage(atalaMessage)) {
                    val credential = CredentialMapper.mapToCredential(receivedMessage, receivedMessage.id, receivedMessage.connectionId, receivedMessage.received.toMilliseconds(), contact)
                    contact.lastMessageId = receivedMessage.id
                    syncLocalDataSource.updateContact(contact, listOf(credential))
                } else if (atalaMessage.messageCase == AtalaMessage.MessageCase.PROOF_REQUEST) {
                    mapProofRequest(atalaMessage.proofRequest, receivedMessage.id, contact.connectionId)?.let {
                        syncLocalDataSource.insertProofRequest(it.first, it.second)
                    }
                    contact.lastMessageId = receivedMessage.id
                    syncLocalDataSource.updateContact(contact, listOf())
                }
            }
        }
    }

    private suspend fun mapProofRequest(proofRequestMessage: io.iohk.atala.prism.protos.ProofRequest, messageId: String, connectionId: String): Pair<ProofRequest, List<Credential>>? {
        val credentials = syncLocalDataSource.credentialsByTypes(proofRequestMessage.typeIdsList)
        val credentialsFound: List<Credential> = proofRequestMessage.typeIdsList.map { typeId ->
            credentials.find { credential -> credential.credentialType == typeId }
        }.filterNotNull()
        // if not all requested credentials are found, the proof request is dismissed
        if (credentialsFound.size == proofRequestMessage.typeIdsList.size) {
            return Pair(
                ProofRequest(connectionId, messageId),
                credentialsFound
            )
        }
        return null
    }

    fun getAllProofRequest(): LiveData<List<ProofRequest>> = syncLocalDataSource.allProofRequest()

    suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials? = syncLocalDataSource.getProofRequestById(id)

    suspend fun removeProofRequest(proofRequest: ProofRequest) = syncLocalDataSource.removeProofRequest(proofRequest)

    suspend fun declineProofRequest(proofRequest: ProofRequest) {
        removeProofRequest(proofRequest)
    }

    suspend fun acceptProofRequest(id: Long) {
        getProofRequestById(id)?.let { proofRequestData ->
            val encodedCredentials = syncLocalDataSource.loadEncodedCredentials(proofRequestData.credentials)
            remoteDataSource.sendCredentialsToContact(proofRequestData.contact!!, encodedCredentials)
            // store activity log
            syncLocalDataSource.insertRequestedCredentialActivities(proofRequestData.contact!!, proofRequestData.credentials)
            removeProofRequest(proofRequestData.proofRequest)
        } ?: kotlin.run {
            throw Exception("ProofRequest nonexistent")
        }
    }
}
