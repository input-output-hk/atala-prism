package io.iohk.cvp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.iohk.cvp.data.DataManager
import io.iohk.cvp.data.local.db.mappers.CredentialMapper
import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.data.local.db.model.Credential
import io.iohk.cvp.grpc.AsyncTaskResult
import io.iohk.cvp.viewmodel.dtos.CredentialsToShare
import io.iohk.prism.protos.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ConnectionsActivityViewModel @Inject constructor(val dataManager: DataManager) : NewConnectionsViewModel(dataManager) {

    private val messages = MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>>()
    private val _credentialsToShare = MutableLiveData<AsyncTaskResult<List<CredentialsToShare?>>>()
    private val _connectionsList = MutableLiveData<AsyncTaskResult<List<Contact?>>>()
    private val _removeAllDataLiveData = MutableLiveData<AsyncTaskResult<Boolean>>()

    fun getCredentialsToShareLiveData(): MutableLiveData<AsyncTaskResult<List<CredentialsToShare?>>> {
        return _credentialsToShare
    }

    fun getContactsLiveData(): MutableLiveData<AsyncTaskResult<List<Contact?>>> {
        return _connectionsList
    }

    fun getAllMessages(): MutableLiveData<AsyncTaskResult<List<ReceivedMessage>>>? {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val connectionList: List<Contact> = dataManager.getAllContacts()
                _connectionsList.postValue(AsyncTaskResult(connectionList))
                /* TODO - When connection is created server take a few milliseconds to create all messages,
                    added a delay to avoid getting empty messages. This should be changed when server implement stream connections */
                delay(TimeUnit.SECONDS.toMillis(1))
                val messagesPaginatedResponseList =  connectionList.asFlow().map { connection ->
                    val messagesList = dataManager.getAllMessages(dataManager.getKeyPairFromPath(connection.keyDerivationPath), connection.lastMessageId).messagesList
                    val credentialList = messagesList.asFlow()
                            .filter {
                                val newMessage: AtalaMessage = AtalaMessage.parseFrom(it.message)
                                newMessage.proofRequest.typeIdsList.isEmpty()
                            } .toList()

                    if(credentialList.isNotEmpty()) {
                        connection.lastMessageId = credentialList.last().id
                        dataManager.updateContact(connection)
                    }
                    return@map messagesList
                }.buffer().flatMapMerge { value ->
                    return@flatMapMerge value.asFlow()
                }.toList()

                if(messagesPaginatedResponseList.isNotEmpty())
                    saveCredentials(messagesPaginatedResponseList)

                val credentialsList = dataManager.getAllCredentials()

                val proofRequests = messagesPaginatedResponseList.filter {
                    receivedMessage ->
                    val newMessage: AtalaMessage = AtalaMessage.parseFrom(receivedMessage.message)
                    newMessage.proofRequest.typeIdsList.isNotEmpty()
                }.map {
                    receivedMessage ->
                    val proofRequest = AtalaMessage.parseFrom(receivedMessage.message).proofRequest
                    val credentialsToShare : MutableList<Credential> = ArrayList()
                    credentialsList.forEach { credential: Credential? ->
                        if (proofRequest.typeIdsList.contains(credential?.credentialType)) {
                            credentialsToShare.add(credential!!)
                        }
                    }

                    if(credentialsToShare.size == proofRequest.typeIdsCount) {
                        val connection = dataManager.getContactByConnectionId(receivedMessage.connectionId)
                        return@map CredentialsToShare(credentialsToShare, connection!!, proofRequest, receivedMessage.id)
                    } else {
                        return@map null
                    }
                }.filterNotNull().toList()

                _credentialsToShare.postValue(AsyncTaskResult(proofRequests))
            } catch (ex:Exception) {
                _credentialsToShare.postValue(AsyncTaskResult(ex))
            }
        }
        return messages
    }

    private suspend fun  saveCredentials(messagesPaginatedResponseList: List<ReceivedMessage>) {
        val credentialsList = messagesPaginatedResponseList.filter {
            val newMessage: AtalaMessage = AtalaMessage.parseFrom(it.message)
            newMessage.proofRequest.typeIdsList.isEmpty()
        }.map {
            receivedMessage: ReceivedMessage? ->
            return@map CredentialMapper.mapToCredential(receivedMessage)
        }.toList()
        dataManager.saveAllCredentials(credentialsList)
    }

    fun removeAllLocalData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataManager.removeAllLocalData()
                _removeAllDataLiveData.postValue(AsyncTaskResult(true))
            } catch (ex:Exception) {
                _removeAllDataLiveData.postValue(AsyncTaskResult(ex))
            }
        }
    }
    fun getRemoveAllDataLiveData(): MutableLiveData<AsyncTaskResult<Boolean>> {
        return _removeAllDataLiveData
    }

    fun clearProofRequestToShow() {
        _credentialsToShare.postValue(AsyncTaskResult(ArrayList()))
    }
}