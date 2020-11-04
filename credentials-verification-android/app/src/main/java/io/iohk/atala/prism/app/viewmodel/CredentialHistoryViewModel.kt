package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.dateFormatDDMMYYYY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CredentialHistoryViewModel(private val dataManager: DataManager) : ViewModel() {

    private val _credential = MutableLiveData<Credential>()

    val credential: LiveData<Credential> = _credential

    private val _activityHistories = MutableLiveData<List<ActivityHistoryWithContact>>(listOf())

    val activityHistories: LiveData<List<ActivityHistoryWithContact>> = _activityHistories

    val formattedIssuedDate = Transformations.map(_credential) {
        dateFormatDDMMYYYY.format(it.dateReceived)
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
}

/**
 * Factory for [CredentialHistoryViewModel].
 * */
class CredentialHistoryViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}