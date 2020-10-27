package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import com.google.protobuf.ByteString
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.mappers.CredentialMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.app.viewmodel.dtos.ConnectionDataDto
import io.iohk.atala.prism.protos.AtalaMessage
import io.iohk.atala.prism.protos.ReceivedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ShareCredentialDialogViewModel(private val dataManager: DataManager) : ViewModel() {

    enum class ErrorType { CantLoadContactsError, CantShareCredentialError }

    private val _error = MutableLiveData<EventWrapper<ErrorType>>()

    val error: LiveData<EventWrapper<ErrorType>> = _error

    val searchText = MutableLiveData<String>("")

    private val _contacts = MutableLiveData<List<CheckableData<Contact>>>(emptyList())

    val filteredContacts = MediatorLiveData<List<CheckableData<Contact>>>().apply {
        value = emptyList()
        addSource(searchText) { value = computeFilteredContacts() }
        addSource(_contacts) { value = computeFilteredContacts() }
    }

    private val _contactsAreLoading = MutableLiveData<Boolean>(false)

    val contactsAreLoading: LiveData<Boolean> = _contactsAreLoading

    private val _credentialSharingIsInProcess = MutableLiveData<Boolean>(false)

    val credentialSharingIsInProcess: LiveData<Boolean> = _credentialSharingIsInProcess

    private val _credentialHasBeenShared = MutableLiveData<EventWrapper<Boolean>>()

    val credentialHasBeenShared: LiveData<EventWrapper<Boolean>> = _credentialHasBeenShared

    // The share button will only be enabled when at least one selected contact is found
    val shareButtonEnabled = Transformations.map(_contacts) {
        return@map it.find { contact -> contact.isChecked } != null
    }

    private lateinit var credential: Credential

    fun fetchData(credentialId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _contactsAreLoading.postValue(true)
                credential = dataManager.getCredentialByCredentialId(credentialId)!!
                val contacts = dataManager.getAllContacts().filter {
                    // you can't share to the contact who issued the current credential
                    it.connectionId != credential.connectionId
                }
                val checkableContacts = contacts.map {
                    CheckableData(it)
                }
                _contacts.postValue(checkableContacts)
            } catch (ex: Exception) {
                _error.postValue(EventWrapper(ErrorType.CantLoadContactsError))
            } finally {
                _contactsAreLoading.postValue(false)
            }
        }
    }

    // is in charge of filtering and sorting the contact list according to the search text
    private fun computeFilteredContacts(): List<CheckableData<Contact>> {
        var result = _contacts.value?.toMutableList() ?: mutableListOf()
        if (searchText.value?.isNotBlank() == true) {
            result = result.filter { contact ->
                contact.data.name.contains(searchText.value!!, ignoreCase = true) ?: false
            }.toMutableList()
        }
        result.sortBy { c -> c.data.name }
        return result
    }

    fun selectDeselectContact(contact: CheckableData<Contact>) {
        contact.setChecked(!contact.isChecked)
        _contacts.value
        _contacts.value = _contacts.value?.toList()
    }

    fun share() {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedCheckableContacts = filteredContacts.value?.filter { it.isChecked }
            if (selectedCheckableContacts?.isNotEmpty() == true) {
                try {
                    val credentialByteArray = ByteString.copyFrom(credential.credentialEncoded.toByteArray())
                    _credentialSharingIsInProcess.postValue(true)
                    // TODO - I think all the following logic should be in a repository
                    val connections = selectedCheckableContacts.map {
                        ConnectionDataDto(it.data.connectionId, dataManager.getKeyPairFromPath(it.data.keyDerivationPath))
                    }
                    dataManager.sendMessageToMultipleConnections(connections, credentialByteArray!!)
                    val selectedContacts = selectedCheckableContacts.map { it.data }
                    dataManager.insertShareCredentialActivityHistories(credential, selectedContacts)
                    /* TODO - When connection is created server take a few milliseconds to create all messages,
                    added a delay to avoid getting empty messages. This should be changed when server implement stream connections */
                    delay(TimeUnit.SECONDS.toMillis(1))
                    connections.forEach { connectionDataDto: ConnectionDataDto ->
                        updateStoredMessages(connectionDataDto.connectionId)
                    }
                    _credentialHasBeenShared.postValue(EventWrapper(true))
                } catch (ex: Exception) {
                    _error.postValue(EventWrapper(ErrorType.CantShareCredentialError))
                } finally {
                    _credentialSharingIsInProcess.postValue(false)
                }
            }
        }
    }

    /*
    * TODO - move this business logic into a repository
    * updateStoredMessages, storeCredentials, updateContactLastMessageSeenId
    * */

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

/**
 * Factory for [ShareCredentialDialogViewModel].
 * */
class ShareCredentialDialogViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}