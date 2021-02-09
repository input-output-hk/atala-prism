package io.iohk.atala.prism.app.ui.main.contacts

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.data.ContactsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class ContactDetailViewModel @Inject constructor(private val repository: ContactsRepository) : ViewModel() {

    private val _contact = MutableLiveData<Contact>()

    val contact: LiveData<Contact> = _contact

    val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = repository.getCustomDateFormat()
        }
    }

    val contactCreatedDate = MediatorLiveData<String>().apply {
        addSource(customDateFormat) { value = computeContactCreatedDate() }
        addSource(contact) { value = computeContactCreatedDate() }
    }

    private val _credentialActivityHistories = MutableLiveData<List<ActivityHistoryWithCredential>>(listOf())

    val credentialActivityHistories: LiveData<List<ActivityHistoryWithCredential>> = _credentialActivityHistories

    fun fetchContact(contactId: Long) {
        viewModelScope.launch {
            repository.getContactById(contactId.toInt())?.let {
                _contact.postValue(it)
                _credentialActivityHistories.postValue(repository.getCredentialsActivityHistories(it.connectionId))
            }
        }
    }

    private fun computeContactCreatedDate(): String? {
        return contact.value?.let {
            customDateFormat.value?.dateFormat?.format(it.dateCreated)
        }
    }
}