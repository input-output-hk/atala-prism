package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeleteContactAlertDialogViewModel(private val dataManager: DataManager) : ViewModel() {

    private val _contact = MutableLiveData<Contact>()

    val contact: LiveData<Contact> = _contact

    private val _credentials = MutableLiveData<List<Credential>>(listOf())

    val credentials: LiveData<List<Credential>> = _credentials

    private val _uiEnabled = MutableLiveData<Boolean>(false)

    val uiEnabled: LiveData<Boolean> = _uiEnabled

    private val _dismiss = MutableLiveData<EventWrapper<Boolean>>()

    val dismiss: LiveData<EventWrapper<Boolean>> = _dismiss


    fun fetchContactInfo(contactId: Int) {
        _uiEnabled.value = false
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.contactById(contactId)?.let {
                _contact.postValue(it)
                _credentials.postValue(dataManager.getCredentialsByConnectionId(it.connectionId))
            }
            _uiEnabled.postValue(true)
        }
    }

    fun deleteContact() {
        contact.value?.let {
            _uiEnabled.value = false
            viewModelScope.launch(Dispatchers.IO) {
                dataManager.deleteCredentialByContactId(it.connectionId)
                dataManager.deleteContact(it)
                _dismiss.postValue(EventWrapper(true))
            }
        }
    }

    fun cancel() {
        _uiEnabled.value = false
        _dismiss.value = EventWrapper(true)
    }
}

/**
 * Factory for [DeleteContactAlertDialogViewModel].
 * */
class DeleteContactAlertDialogViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}