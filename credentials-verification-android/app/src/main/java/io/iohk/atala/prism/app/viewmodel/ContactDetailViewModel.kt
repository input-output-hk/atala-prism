package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactDetailViewModel(private val dataManager: DataManager) : ViewModel() {

    private val _contact = MutableLiveData<Contact>()

    val contact: LiveData<Contact> = _contact

    val contactCreatedDate: LiveData<String> = Transformations.map(contact) {
        dateFormatDDMMYYYY.format(it.dateCreated)
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
}

/**
 * Factory for [ContactDetailViewModel].
 * */
class ContactDetailViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}