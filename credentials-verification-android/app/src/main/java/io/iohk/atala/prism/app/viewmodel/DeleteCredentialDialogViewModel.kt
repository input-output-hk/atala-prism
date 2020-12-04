package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeleteCredentialDialogViewModel(private val dataManager: DataManager) : ViewModel() {

    private val _credential = MutableLiveData<Credential>()

    val credential: LiveData<Credential> = _credential

    private val _canceled = MutableLiveData<EventWrapper<Boolean>>()

    val canceled: LiveData<EventWrapper<Boolean>> = _canceled

    private val _credentialDeleted = MutableLiveData<EventWrapper<Boolean>>()

    val credentialDeleted: LiveData<EventWrapper<Boolean>> = _credentialDeleted

    fun fetchCredentialInfo(credentialId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getCredentialByCredentialId(credentialId)?.let {
                _credential.postValue(it)
            }
        }
    }

    fun deleteCredential() {
        _credential.value?.let {
            viewModelScope.launch(Dispatchers.IO) {
                dataManager.deleteCredential(it)
                _credentialDeleted.postValue(EventWrapper(true))
            }
        }
    }

    fun cancel() {
        _canceled.value = EventWrapper(true)
    }
}

/**
 * Factory for [DeleteCredentialDialogViewModel].
 * */
class DeleteCredentialDialogViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}