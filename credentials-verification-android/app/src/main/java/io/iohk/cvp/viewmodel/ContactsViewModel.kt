package io.iohk.cvp.viewmodel

import androidx.lifecycle.*
import io.iohk.cvp.data.DataManager
import io.iohk.cvp.data.local.db.model.Contact

class ContactsViewModel(private val dataManager: DataManager) : ViewModel() {
    
    val searchText = MutableLiveData<String>("")

    private val _contacts = dataManager.allContacts()

    val contacts = MediatorLiveData<List<Contact>>().apply {
        value = listOf()
        addSource(_contacts) { value = computeFilteredContacts() }
        addSource(searchText) { value = computeFilteredContacts() }
    }

    // is in charge of filtering the contact list according to the search text
    private fun computeFilteredContacts(): List<Contact> {
        var result = _contacts.value?.toMutableList() ?: mutableListOf()
        if (searchText.value?.isNotBlank() == true) {
            result = result.filter { contact ->
                contact.name.contains(searchText.value!!, ignoreCase = true) ?: false
            }.toMutableList()
        }
        result.sortBy { it.name }
        return result
    }
}

/**
 * Factory for [ContactsViewModel].
 * */
class ContactsViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}