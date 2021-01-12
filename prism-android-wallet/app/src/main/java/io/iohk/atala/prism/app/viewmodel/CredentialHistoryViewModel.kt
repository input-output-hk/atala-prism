package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CredentialHistoryViewModel(private val dataManager: DataManager) : ViewModel() {

    private val _credential = MutableLiveData<Credential>()

    val credential: LiveData<Credential> = _credential

    private val _activityHistories = MutableLiveData<List<ActivityHistoryWithContact>>(listOf())

    val activityHistories: LiveData<List<ActivityHistoryWithContact>> = _activityHistories

    val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = dataManager.getCurrentDateFormat()
        }
    }

    val formattedIssuedDate = MediatorLiveData<String>().apply {
        addSource(customDateFormat) { value = computeContactCreatedDate() }
        addSource(_credential) { value = computeContactCreatedDate() }
    }

    fun fetchData(credentialId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getCredentialByCredentialId(credentialId)?.let { credential ->
                _credential.postValue(credential)
                // Only need shared and requested activity stories
                val activityHistories = dataManager.getContactsActivityHistoriesByCredentialId(credentialId).filter {
                    it.activityHistory.type == ActivityHistory.Type.CredentialShared || it.activityHistory.type == ActivityHistory.Type.CredentialRequested
                }
                _activityHistories.postValue(activityHistories)
            }
        }
    }

    private fun computeContactCreatedDate(): String? {
        val customDateFormat = customDateFormat.value ?: dataManager.getDefaultDateFormat()
        return if (_credential.value == null) null else customDateFormat.dateFormat.format(_credential.value!!.dateReceived)
    }
}

/**
 * Factory for [CredentialHistoryViewModel].
 * */
class CredentialHistoryViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}