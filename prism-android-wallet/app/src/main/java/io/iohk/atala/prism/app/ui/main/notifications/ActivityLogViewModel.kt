package io.iohk.atala.prism.app.ui.main.notifications

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import kotlinx.coroutines.launch
import javax.inject.Inject

class ActivityLogViewModel @Inject constructor(dataManager: DataManager) : ViewModel() {
    val activityHistories: LiveData<List<ActivityHistoryWithContactAndCredential>> = dataManager.activityHistories()

    val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = dataManager.getCurrentDateFormat()
        }
    }
}