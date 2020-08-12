package io.iohk.cvp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.protobuf.ByteString
import io.iohk.cvp.data.DataManager
import io.iohk.cvp.data.local.db.mappers.CredentialMapper
import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.grpc.AsyncTaskResult
import io.iohk.cvp.viewmodel.dtos.ConnectionListable
import io.iohk.prism.protos.AtalaMessage
import io.iohk.prism.protos.ReceivedMessage
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ConnectionsListablesViewModel() : CvpViewModel() {

    private val _connectionsLiveData = MutableLiveData<AsyncTaskResult<List<Contact?>>>()
    private val _messageSentLiveData = MutableLiveData<AsyncTaskResult<Boolean>>()
    private val _connectionIdUpdatedLiveData = MutableLiveData<AsyncTaskResult<Boolean>>()

    private lateinit var dataManager : DataManager

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

    fun sendMultipleMessage(senderUserId: String, connectionId: String,
                            messages: List<ByteString>, lastMessageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataManager.sendMultipleMessage(senderUserId, connectionId, messages)
                /* TODO - When connection is created server take a few milliseconds to create all messages,
                    added a delay to avoid getting empty messages. This should be changed when server implement stream connections */
                delay(TimeUnit.SECONDS.toMillis(1))
                updateStoredMessages(connectionId)
                _messageSentLiveData.postValue(AsyncTaskResult(true))
            } catch (ex:Exception) {
                _messageSentLiveData.postValue(AsyncTaskResult(ex))
            }
        }
    }

    private suspend fun updateStoredMessages(connectionId: String) {
        val contact = dataManager.getContactByConnectionId(connectionId)!!
        val messagesList = dataManager.getAllMessages(contact.userId, contact.lastMessageId).messagesList
        val credentialList = messagesList.filter {
            val newMessage: AtalaMessage = AtalaMessage.parseFrom(it.message)
            newMessage.proofRequest.typeIdsList.isEmpty()
        }
        if(credentialList.isNotEmpty()) {
            storeCredentials(credentialList)
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
            } catch (ex:Exception) {
                _connectionIdUpdatedLiveData.postValue(AsyncTaskResult(ex))
            }
        }

    }

    private suspend fun storeCredentials(credentialList: List<ReceivedMessage>) {
        val credentialToStore = credentialList.map {
            receivedMessage: ReceivedMessage? ->
            return@map CredentialMapper.mapToCredential(receivedMessage)
        }.toList()
        dataManager.saveAllCredentials(credentialToStore)
    }

    private suspend fun updateContactLastMessageSeenId(connectionId: String, messageId: String?) {
        val contact = dataManager.getContactByConnectionId(connectionId)
        contact?.lastMessageId = messageId
        dataManager.updateContact(contact!!)
    }

    fun allConnectionsLiveData(): MutableLiveData<AsyncTaskResult<List<Contact?>>> {
        return _connectionsLiveData
    }

    fun sendMessageToMultipleConnections(selectedVerifiers: MutableSet<ConnectionListable>, byteString: ByteString) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataManager.sendMessageToMultipleConnections(selectedVerifiers, byteString)
                /* TODO - When connection is created server take a few milliseconds to create all messages,
                    added a delay to avoid getting empty messages. This should be changed when server implement stream connections */
                delay(TimeUnit.SECONDS.toMillis(1))
                selectedVerifiers.forEach { connectionListable: ConnectionListable ->
                    updateStoredMessages(connectionListable.connectionIdValue)
                }
                _messageSentLiveData.postValue(AsyncTaskResult(true))
            } catch (ex:Exception) {
                _messageSentLiveData.postValue(AsyncTaskResult(ex))
            }
        }
    }

    fun getAllConnections() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _connectionsLiveData.postValue(AsyncTaskResult(dataManager.getAllContacts()))
            } catch (ex:Exception) {
                _connectionsLiveData.postValue(AsyncTaskResult(ex))
            }
        }
    }

    fun shouldNotShowSuccessDialog() {
        _connectionIdUpdatedLiveData.postValue(AsyncTaskResult(false))
    }
}