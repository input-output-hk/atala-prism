package io.iohk.atala.prism.app.ui.main.contacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class DeleteContactAlertDialogViewModel @Inject constructor(
    private val repository: ContactsRepository
) : ViewModel() {

    private val _contact = MutableLiveData<Contact>()

    val contact: LiveData<Contact> = _contact

    private val _credentials = MutableLiveData<List<Credential>>(listOf())

    val credentials: LiveData<List<Credential>> = _credentials

    private val _uiEnabled = MutableLiveData<Boolean>(false)

    val uiEnabled: LiveData<Boolean> = _uiEnabled

    private val _dismiss = MutableLiveData<EventWrapper<Boolean>>()

    val dismiss: LiveData<EventWrapper<Boolean>> = _dismiss

    private val _contactDeleted = MutableLiveData<EventWrapper<Boolean>>()

    val contactDeleted: LiveData<EventWrapper<Boolean>> = _contactDeleted

    fun fetchContactInfo(contactId: Int) {
        _uiEnabled.value = false
        viewModelScope.launch {
            repository.getContactById(contactId)?.let {
                _contact.postValue(it)
                _credentials.postValue(repository.getIssuedCredentials(it.connectionId))
            }
            _uiEnabled.postValue(true)
        }
    }

    fun deleteContact() {
        contact.value?.let {
            _uiEnabled.value = false
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteContact(it)
                _contactDeleted.postValue(EventWrapper(true))
            }
        }
    }

    fun cancel() {
        _uiEnabled.value = false
        _dismiss.value = EventWrapper(true)
    }
}
