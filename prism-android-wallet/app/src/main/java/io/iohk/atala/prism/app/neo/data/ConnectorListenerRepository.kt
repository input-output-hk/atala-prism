package io.iohk.atala.prism.app.neo.data

import android.util.Log
import androidx.lifecycle.Observer
import io.iohk.atala.prism.app.data.local.db.mappers.CredentialMapper
import io.iohk.atala.prism.app.data.local.db.mappers.KycRequestMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.data.local.db.model.PayIdPublicKey
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.neo.common.extensions.toMilliseconds
import io.iohk.atala.prism.app.neo.data.local.ConnectorListenerLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorGRPCStream
import io.iohk.atala.prism.app.neo.data.remote.ConnectorGRPCStreamsManager
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.protos.AtalaMessage
import io.iohk.atala.prism.protos.MirrorMessage
import io.iohk.atala.prism.protos.ReceivedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConnectorListenerRepository(
    private val localDataSource: ConnectorListenerLocalDataSourceInterface,
    private val remoteDataSource: ConnectorRemoteDataSource,
    private val mirrorMessageReceiver: MirrorMessageReceiver,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) :
    BaseRepository(
        sessionLocalDataSource,
        preferencesLocalDataSource
    ) {

    private val contactsData = localDataSource.allContacts()

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
        try {
            CoroutineScope(Dispatchers.Default).launch {
                localDataSource.getContactByConnectionId(connectionId)?.let { contact ->
                    val atalaMessage = AtalaMessage.parseFrom(receivedMessage.message)
                    if (atalaMessage.messageCase == AtalaMessage.MessageCase.PLAIN_CREDENTIAL) {
                        val credential = CredentialMapper.mapToCredential(
                            receivedMessage,
                            receivedMessage.id,
                            receivedMessage.connectionId,
                            receivedMessage.received.toMilliseconds(),
                            contact
                        )
                        contact.lastMessageId = receivedMessage.id
                        localDataSource.updateContact(contact, listOf(credential))
                    } else if (atalaMessage.messageCase == AtalaMessage.MessageCase.PROOF_REQUEST) {
                        mapProofRequest(atalaMessage.proofRequest, receivedMessage.id, contact.connectionId)?.let {
                            localDataSource.insertProofRequest(it.first, it.second)
                        }
                        contact.lastMessageId = receivedMessage.id
                        localDataSource.updateContact(contact, listOf())
                    } else if (atalaMessage.messageCase == AtalaMessage.MessageCase.KYC_BRIDGE_MESSAGE) {
                        KycRequestMapper.map(contact.connectionId, receivedMessage.id, atalaMessage.kycBridgeMessage)
                            ?.let {
                                localDataSource.storeKycRequest(it)
                                contact.lastMessageId = receivedMessage.id
                                localDataSource.updateContact(contact, listOf())
                            }
                    } else {
                        handleMirrorMessages(atalaMessage, receivedMessage.id)
                        contact.lastMessageId = receivedMessage.id
                        localDataSource.updateContact(contact, listOf())
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("Error in parsePlainTextCredential:", ex.message)
            ex.printStackTrace()
        }
    }

    private suspend fun mapProofRequest(
        proofRequestMessage: io.iohk.atala.prism.protos.ProofRequest,
        messageId: String,
        connectionId: String
    ): Pair<ProofRequest, List<Credential>>? {
        val credentials = localDataSource.credentialsByTypes(proofRequestMessage.typeIdsList)
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

    private suspend fun handleMirrorMessages(atalaMessage: AtalaMessage, messageId: String) {
        // see if there is a PayId waiting for response
        if (atalaMessage.replyTo.isNotBlank()) {
            localDataSource.getPayIdByMessageIdAndStatus(atalaMessage.replyTo, PayId.Status.WaitingForResponse)
                ?.let { payId ->
                    if (atalaMessage.mirrorMessage.messageCase == MirrorMessage.MessageCase.PAYID_NAME_REGISTERED_MESSAGE) {
                        payId.status = PayId.Status.Registered
                        localDataSource.updatePayId(payId)
                    } else {
                        localDataSource.deletePayId(payId)
                    }
                    // Message is sent to an a mirrorMessageReceiver, it will handle the scenarios in which the UI is waiting for a response
                    mirrorMessageReceiver.handleNewReceivedMessage(atalaMessage)
                    return@handleMirrorMessages
                }
        }
        localDataSource.getPayId()?.let { payId ->
            when (atalaMessage.mirrorMessage.messageCase) {
                MirrorMessage.MessageCase.ADDRESS_REGISTERED_MESSAGE -> {
                    val payIdAddress = PayIdAddress(
                        payId.id,
                        atalaMessage.mirrorMessage.addressRegisteredMessage.cardanoAddress,
                        messageId
                    )
                    localDataSource.createPayIdAddress(payIdAddress)
                }
                MirrorMessage.MessageCase.WALLET_REGISTERED -> {
                    val payIdPublicKey = PayIdPublicKey(
                        payId.id,
                        atalaMessage.mirrorMessage.walletRegistered.extendedPublicKey,
                        messageId
                    )
                    localDataSource.createPayIdPublicKey(payIdPublicKey)
                }
                else -> {
                }
            }
        }
        // Message is sent to an a mirrorMessageReceiver, it will handle the scenarios in which the UI is waiting for a response
        mirrorMessageReceiver.handleNewReceivedMessage(atalaMessage)
    }
}
