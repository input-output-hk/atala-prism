package io.iohk.atala.prism.app.ui.main.notifications

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.data.ActivityHistoriesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class ActivityLogViewModel @Inject constructor(private val repository: ActivityHistoriesRepository) : ViewModel() {
    val activityHistories: LiveData<List<ActivityHistoryWithContactAndCredential>> = repository.allActivityHistories()

    val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = repository.getCustomDateFormat()
        }
    }
}