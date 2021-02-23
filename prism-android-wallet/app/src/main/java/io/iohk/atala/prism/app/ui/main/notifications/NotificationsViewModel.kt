package io.iohk.atala.prism.app.ui.main.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.data.ActivityHistoriesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationsViewModel @Inject constructor(
    private val repository: ActivityHistoriesRepository
) : ViewModel() {

    val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = repository.getCustomDateFormat()
        }
    }

    /**
     * There is no model for notifications, a notification is actually a record in the "activityHistories" table with the "type" field equal to [ActivityHistory.Type.CredentialIssued]
     * and the [ActivityHistory.needsToBeNotified] field equal to true. you can see the table fields in class [ActivityHistory] and expect to receive a list of type [ActivityHistoryWithCredential]
     * containing the data of an [ActivityHistory] along with its related [Credential]
     * */
    val notifications: LiveData<List<ActivityHistoryWithCredential>> = repository.allIssuedCredentialsNotifications()

    val showEmptyMessage: LiveData<Boolean> = Transformations.map(notifications) {
        it.isEmpty()
    }

    /**
     * When there are no stored contacts it means that there are no connections and all local data is empty, therefore the view that invites
     * to create the first connection should be shown
     * */
    val showNoConnectionsView: LiveData<Boolean> = Transformations.map(repository.areThereConnections()) {
        !it
    }
}
