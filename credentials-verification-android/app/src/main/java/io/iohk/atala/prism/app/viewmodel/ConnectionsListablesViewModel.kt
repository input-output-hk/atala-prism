package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.mappers.CredentialMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.grpc.AsyncTaskResult
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.protos.AtalaMessage
import io.iohk.atala.prism.protos.ReceivedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.inject.Inject

class ConnectionsListablesViewModel() : CvpViewModel() {

    private val _messageSentLiveData = MutableLiveData<AsyncTaskResult<Boolean>>(AsyncTaskResult(false))
    private val _connectionIdUpdatedLiveData = MutableLiveData<AsyncTaskResult<Boolean>>()

    private lateinit var dataManager: DataManager

    @Inject
    constructor(dataManager: DataManager) : this() {
        this.dataManager = dataManager
    }

    fun getMessageSentLiveData(): LiveData<AsyncTaskResult<Boolean>>? {
        return _messageSentLiveData
    }

    fun getContactUpdatedLiveData(): LiveData<AsyncTaskResult<Boolean>>? {
        return _connectionIdUpdatedLiveData
    }

    fun acceptProofRequest(path: String, contact: Contact,
                           credentials: List<Credential>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = credentials.stream().map {
                    it.credentialEncoded
                }.collect(Collectors.toList())
                dataManager.sendMultipleMessage(dataManager.getKeyPairFromPath(path), contact.connectionId, messages)
                dataManager.insertRequestedCredentialActivities(contact, credentials)
                /* TODO - When connection is created server take a few milliseconds to create all messages,
                    added a delay to avoid getting empty messages. This should be changed when server implement stream connections */
                delay(TimeUnit.SECONDS.toMillis(1))
                updateStoredMessages(contact.connectionId)
                _messageSentLiveData.postValue(AsyncTaskResult(true))
            } catch (ex: Exception) {
                _messageSentLiveData.postValue(AsyncTaskResult(ex))
            }
        }
    }

    private suspend fun updateStoredMessages(connectionId: String) {
        val contact = dataManager.getContactByConnectionId(connectionId)!!
        val messagesList = dataManager.getAllMessages(CryptoUtils.getKeyPairFromPath(contact.keyDerivationPath, dataManager.getMnemonicList()), contact.lastMessageId).messagesList
        val credentialList = messagesList.filter {
            val newMessage: AtalaMessage = AtalaMessage.parseFrom(it.message)
            newMessage.proofRequest.typeIdsList.isEmpty()
        }
        if (credentialList.isNotEmpty()) {
            storeCredentials(contact.id, credentialList)
            updateContactLastMessageSeenId(connectionId, credentialList.last().id)
        } else {
            updateContactLastMessageSeenId(connectionId, contact.lastMessageId)
        }
    }

    fun setLastMessageSeen(connectionId: String, messageId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateContactLastMessageSeenId(connectionId, messageId)
                _connectionIdUpdatedLiveData.postValue(AsyncTaskResult(true))
            } catch (ex: Exception) {
                _connectionIdUpdatedLiveData.postValue(AsyncTaskResult(ex))
            }
        }

    }

    private suspend fun storeCredentials(contactId: Long, credentialList: List<ReceivedMessage>) {
        val credentialToStore = credentialList.map { receivedMessage: ReceivedMessage? ->
            return@map CredentialMapper.mapToCredential(receivedMessage)
        }.toList()
        dataManager.insertIssuedCredentialsToAContact(contactId, credentialToStore)
    }

    private suspend fun updateContactLastMessageSeenId(connectionId: String, messageId: String?) {
        val contact = dataManager.getContactByConnectionId(connectionId)
        contact?.lastMessageId = messageId
        dataManager.updateContact(contact!!)
    }
}