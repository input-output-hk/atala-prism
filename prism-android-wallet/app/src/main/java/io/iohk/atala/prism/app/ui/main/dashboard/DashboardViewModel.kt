package io.iohk.atala.prism.app.ui.main.dashboard

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.data.DashboardRepository
import io.iohk.atala.prism.app.neo.model.DashboardNotification
import io.iohk.atala.prism.app.neo.model.UserProfile
import kotlinx.coroutines.launch
import javax.inject.Inject

class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    companion object {
        private const val MAX_LAST_ACTIVITY_HISTORIES = 3
    }

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
    val totalOfNotifications: LiveData<Int> = repository.totalOfIssuedCredentialsNotifications

    val lastActivitiesHistories: LiveData<List<ActivityHistoryWithContactAndCredential>> = repository.lastActivityHistories(MAX_LAST_ACTIVITY_HISTORIES)

    private val userProfile: MutableLiveData<UserProfile?> = MutableLiveData()

    val userProfileName: LiveData<String> = Transformations.map(userProfile) {
        it?.name
    }

    val userProfileImage: LiveData<Bitmap> = Transformations.map(userProfile) {
        it?.profileImage
    }

    val dashboardCardNotifications: LiveData<List<DashboardNotification>> = repository.dashboardCardNotifications

    fun loadData() {
        viewModelScope.launch {
            userProfile.value = repository.getUserProfile()
        }
    }

    fun removeDashboardNotification(notification: DashboardNotification) {
        viewModelScope.launch {
            repository.removeDashboardCardNotification(notification)
        }
    }
}
