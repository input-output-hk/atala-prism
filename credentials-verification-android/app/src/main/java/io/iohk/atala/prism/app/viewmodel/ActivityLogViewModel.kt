package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import kotlinx.coroutines.launch

class ActivityLogViewModel(dataManager: DataManager) : ViewModel() {
    val activityHistories: LiveData<List<ActivityHistoryWithContactAndCredential>> = dataManager.activityHistories()

    val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = dataManager.getCurrentDateFormat()
        }
    }
}

/**
 * Factory for [ActivityLogViewModel].
 * */
class ActivityLogViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}