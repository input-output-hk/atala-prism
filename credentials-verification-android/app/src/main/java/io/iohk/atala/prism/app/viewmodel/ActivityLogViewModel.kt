package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential

class ActivityLogViewModel(dataManager: DataManager) : ViewModel() {
    val activityHistories: LiveData<List<ActivityHistoryWithContactAndCredential>> = dataManager.activityHistories()
}

/**
 * Factory for [ActivityLogViewModel].
 * */
class ActivityLogViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}