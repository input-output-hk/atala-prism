package io.iohk.atala.prism.app.ui.main.contacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.neo.data.ContactsRepository
import javax.inject.Inject

class ContactsViewModel @Inject constructor(private val repository: ContactsRepository) : ViewModel() {

    val searchText = MutableLiveData<String>("")

    private val _contacts = repository.allContacts()

    val contacts = MediatorLiveData<List<Contact>>().apply {
        value = listOf()
        addSource(_contacts) { value = computeFilteredContacts() }
        addSource(searchText) { value = computeFilteredContacts() }
    }

    /**
     * When there are no stored contacts it means that there are no connections and all local data is empty, therefore the view that invites
     * to create the first connection should be shown
     * */
    val showNoConnectionsView: LiveData<Boolean> = Transformations.map(_contacts) {
        it.isEmpty()
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
