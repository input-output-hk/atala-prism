package io.iohk.cvp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.iohk.cvp.data.DataManager
import io.iohk.cvp.data.local.db.mappers.ContactMapper
import io.iohk.cvp.data.local.db.mappers.CredentialMapper
import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.data.local.db.model.Credential
import io.iohk.cvp.grpc.AsyncTaskResult
import io.iohk.cvp.utils.CryptoUtils
import io.iohk.atala.prism.protos.AddConnectionFromTokenResponse
import io.iohk.cvp.viewmodel.dtos.CredentialsToShare
import io.iohk.atala.prism.protos.AtalaMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(private val dataManager: DataManager) : NewConnectionsViewModel(dataManager) {

    private val _newConnectionInfoLiveData = MutableLiveData<AsyncTaskResult<AddConnectionFromTokenResponse>>()
    private val _hasConnectionsInitialScreenLiveData = MutableLiveData<AsyncTaskResult<Boolean>>()
    private val _hasConnectionsMoveToContact = MutableLiveData<AsyncTaskResult<Boolean>>()

    fun addConnectionFromToken(token: String, nonce: String)
            : LiveData<AsyncTaskResult<AddConnectionFromTokenResponse>>? {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentIndex = dataManager.getCurrentIndex()
                val addConnectionFromTokenResponse = dataManager.addConnection(dataManager.getKeyPairFromPath(CryptoUtils.getNextPathFromIndex(currentIndex)), token, nonce)
                dataManager.saveContact(ContactMapper.mapToContact(addConnectionFromTokenResponse.connection, CryptoUtils.getNextPathFromIndex(currentIndex)))
                dataManager.increaseIndex()
                _newConnectionInfoLiveData.postValue(AsyncTaskResult(addConnectionFromTokenResponse))
            } catch (ex: Exception) {
                _newConnectionInfoLiveData.postValue(AsyncTaskResult(ex))
            }
        }
        return _newConnectionInfoLiveData
    }

    fun checkIfHasConnectionsInitialScreen() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _hasConnectionsInitialScreenLiveData.postValue(getConnections())
            } catch (ex: Exception) {
                _hasConnectionsInitialScreenLiveData.postValue(AsyncTaskResult(ex))
            }
        }
    }

    fun checkIfHasConnectionsMoveToContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _hasConnectionsMoveToContact.postValue(getConnections())
            } catch (ex: Exception) {
                _hasConnectionsMoveToContact.postValue(AsyncTaskResult(ex))
            }
        }
    }

    private suspend fun getConnections(): AsyncTaskResult<Boolean> {
        val connectionsList = dataManager.getAllContacts()
        return AsyncTaskResult(connectionsList.isEmpty())
    }

    fun getHasConnectionsInitialScreenLiveData(): LiveData<AsyncTaskResult<Boolean>> {
        return _hasConnectionsInitialScreenLiveData
    }

    fun getHasConnectionsMoveToContactLiveData(): LiveData<AsyncTaskResult<Boolean>> {
        return _hasConnectionsMoveToContact
    }


    /*
    * TODO All this below is a provisional code, it is necessary to create a data synchronization repository
    *  which handles all this logic, insert and delete data through the Room library, and take advantage
    *  of the integration of room with LiveData and in this way create a more reactive application.
    * */

    private val _credentialsRequests = MutableLiveData<List<CredentialsToShare>>(listOf())

    val credentialsRequests: LiveData<List<CredentialsToShare>> = _credentialsRequests

    private var isSyncing: Boolean = false

    fun syncMessages() {
        if (isSyncing) {
            return
        }
        isSyncing = true
        viewModelScope.launch(Dispatchers.IO) {
            // TODO This delay is here because after a connection is confirmed it, the server takes a few milliseconds to create the messages with the proof requests
            delay(1000)
            val contacts = dataManager.getAllContacts()
            val atalaMessageProofRequests = mutableMapOf<AtalaMessage, Pair<Contact, String>>()
            contacts.forEach { contact ->
                val credentialsToStore: MutableList<Credential> = mutableListOf()
                val keyPair = dataManager.getKeyPairFromPath(contact.keyDerivationPath)
                val messages = dataManager.getAllMessages(keyPair, contact.lastMessageId).messagesList
                messages.forEach { receivedMessage ->
                    val atalaMessage = AtalaMessage.parseFrom(receivedMessage.message)
                    // find if it is a credential or a proof request
                    if (atalaMessage.proofRequest.typeIdsList.isEmpty()) {
                        // its a Credential
                        val credential = CredentialMapper.mapToCredential(receivedMessage)
                        credentialsToStore.add(credential)
                    } else {
                        // its a Proof request
                        atalaMessageProofRequests[atalaMessage] = Pair(contact, receivedMessage.id)
                        //atalaMessageProofRequests.add(atalaMessage)
                    }
                    contact.lastMessageId = receivedMessage.id
                }
                // store credentials locally
                dataManager.saveAllCredentials(credentialsToStore)
                // TODO I think we need another model to store all Proof requests locally
                dataManager.updateContact(contact)
            }
            // I'm going to check if the proof requests match the credentials that currently exist locally
            val credentials = dataManager.getAllCredentials()
            val credentialsToShareList: MutableList<CredentialsToShare> = mutableListOf()
            atalaMessageProofRequests.forEach { (atalaMessage, contactAndMessageId) ->
                val credentialsFound: List<Credential> = atalaMessage.proofRequest.typeIdsList.map { typeId ->
                    credentials.find { credential -> credential.credentialType == typeId }
                }.filterNotNull()
                val contact = contactAndMessageId.first
                val messageId = contactAndMessageId.second
                if (credentialsFound.size == atalaMessage.proofRequest.typeIdsList.size) {
                    val credentialsToShare = CredentialsToShare(
                            credentialsFound,
                            contact,
                            atalaMessage.proofRequest,
                            messageId)
                    credentialsToShareList.add(credentialsToShare)
                } else {
                    // TODO it remains to establish what to do when the total of the requested credentials is not found
                }
            }

            // TODO in this point we have all the messages with proof requests and for now I'm just going to map them and expose them in a livedata so that the view shows the user the option to accept or reject
            _credentialsRequests.postValue(credentialsToShareList)
            isSyncing = false
        }
    }
}