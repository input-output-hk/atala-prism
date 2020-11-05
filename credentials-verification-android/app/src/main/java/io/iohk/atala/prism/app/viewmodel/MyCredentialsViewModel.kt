package io.iohk.atala.prism.app.viewmodel

import android.content.res.Resources
import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.views.fragments.CredentialUtil

class MyCredentialsViewModel(private val dataManager: DataManager, private val resources: Resources) : ViewModel() {

    val searchText = MutableLiveData<String>("")

    private val _credentials: LiveData<List<Credential>> = dataManager.allCredentials()

    val filteredCredentials: LiveData<List<Credential>> = MediatorLiveData<List<Credential>>().apply {
        addSource(searchText) { value = computeCredentials() }
        addSource(_credentials) { value = computeCredentials() }
    }

    val showNoResultMessage = MediatorLiveData<Boolean>().apply {
        addSource(filteredCredentials) { value = computeNoResultView() }
        addSource(searchText) { value = computeNoResultView() }
        addSource(_credentials) { value = computeNoResultView() }
    }

    val showNoCredentialsMessage: LiveData<Boolean> = Transformations.map(_credentials) {
        it.isEmpty()
    }

    private fun computeCredentials(): List<Credential> {
        var result = _credentials.value ?: listOf()
        if (searchText.value?.isNotBlank() == true) {
            result = result.filter { credential ->
                val credentialName = resources.getString(CredentialUtil.getNameResource(credential))
                credential.issuerName.contains(searchText.value!!, ignoreCase = true) || credentialName.contains(searchText.value!!, ignoreCase = true)
            }.toList()
        }
        return result
    }

    private fun computeNoResultView(): Boolean {
        return searchText.value?.isNotBlank() == true
                && filteredCredentials.value?.size == 0
                && _credentials.value?.isNotEmpty() == true
    }
}

/**
 * Factory for [MyCredentialsViewModel].
 * */
class MyCredentialsViewModelFactory(private val dataManager: DataManager, private val resources: Resources) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MyCredentialsViewModel(dataManager, resources) as T
    }
}