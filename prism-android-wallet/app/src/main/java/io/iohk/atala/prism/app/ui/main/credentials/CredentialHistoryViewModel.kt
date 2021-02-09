package io.iohk.atala.prism.app.ui.main.credentials

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.data.CredentialsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class CredentialHistoryViewModel @Inject constructor(private val repository: CredentialsRepository) : ViewModel() {

    private val _credential = MutableLiveData<Credential>()

    val credential: LiveData<Credential> = _credential

    private val _activityHistories = MutableLiveData<List<ActivityHistoryWithContact>>(listOf())

    val activityHistories: LiveData<List<ActivityHistoryWithContact>> = _activityHistories

    val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = repository.getCustomDateFormat()
        }
    }

    val formattedIssuedDate = MediatorLiveData<String>().apply {
        addSource(customDateFormat) { value = computeContactCreatedDate() }
        addSource(_credential) { value = computeContactCreatedDate() }
    }

    fun fetchData(credentialId: String) {
        viewModelScope.launch {
            repository.getCredentialByCredentialId(credentialId)?.let { credential ->
                _credential.postValue(credential)
                // Only need shared and requested activity stories
                val activityHistories = repository.getContactsActivityHistoriesByCredentialId(credentialId).filter {
                    it.activityHistory.type == ActivityHistory.Type.CredentialShared || it.activityHistory.type == ActivityHistory.Type.CredentialRequested
                }
                _activityHistories.postValue(activityHistories)
            }
        }
    }

    private fun computeContactCreatedDate(): String? {
        return _credential.value?.let {
            customDateFormat.value?.dateFormat?.format(it.dateReceived)
        }
    }
}