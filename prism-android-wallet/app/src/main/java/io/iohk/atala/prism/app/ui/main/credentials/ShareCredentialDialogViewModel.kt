package io.iohk.atala.prism.app.ui.main.credentials

import androidx.lifecycle.*
import com.google.protobuf.ByteString
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import io.iohk.atala.prism.app.data.dtos.ConnectionDataDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ShareCredentialDialogViewModel @Inject constructor(private val dataManager: DataManager) : ViewModel() {

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
                    _credentialHasBeenShared.postValue(EventWrapper(true))
                } catch (ex: Exception) {
                    _error.postValue(EventWrapper(ErrorType.CantShareCredentialError))
                } finally {
                    _credentialSharingIsInProcess.postValue(false)
                }
            }
        }
    }
}