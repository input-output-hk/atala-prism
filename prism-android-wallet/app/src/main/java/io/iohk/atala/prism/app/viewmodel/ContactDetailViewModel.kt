package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactDetailViewModel(private val dataManager: DataManager) : ViewModel() {

    private val _contact = MutableLiveData<Contact>()

    val contact: LiveData<Contact> = _contact

    val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = dataManager.getCurrentDateFormat()
        }
    }

    val contactCreatedDate = MediatorLiveData<String>().apply {
        addSource(customDateFormat) { value = computeContactCreatedDate() }
        addSource(contact) { value = computeContactCreatedDate() }
    }

    private val _credentialActivityHistories = MutableLiveData<List<ActivityHistoryWithCredential>>(listOf())

    val credentialActivityHistories: LiveData<List<ActivityHistoryWithCredential>> = _credentialActivityHistories

    fun fetchContact(contactId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.contactById(contactId.toInt())?.let {
                _contact.postValue(it)
                _credentialActivityHistories.postValue(dataManager.getCredentialsActivityHistoriesByConnection(it.connectionId))
            }
        }
    }

    private fun computeContactCreatedDate(): String? {
        val customDateFormat = customDateFormat.value ?: dataManager.getDefaultDateFormat()
        return if (contact.value == null) null else customDateFormat.dateFormat.format(contact.value!!.dateCreated)
    }
}

/**
 * Factory for [ContactDetailViewModel].
 * */
class ContactDetailViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}