package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.Credential

class NotificationsViewModel(private val dataManager: DataManager) : ViewModel() {

    private val totalOfContacts = dataManager.totalOfContacts()

    /**
     * There is no model for notifications, a notification is actually a record in the "activityHistories" table with the "type" field equal to [ActivityHistory.Type.CredentialIssued]
     * and the [ActivityHistory.needsToBeNotified] field equal to true. you can see the table fields in class [ActivityHistory] and expect to receive a list of type [ActivityHistoryWithCredential]
     * containing the data of an [ActivityHistory] along with its related [Credential]
     * */
    val notifications: LiveData<List<ActivityHistoryWithCredential>> = dataManager.allIssuedCredentialsNotifications()

    val showEmptyMessage: LiveData<Boolean> = Transformations.map(notifications) {
        it.isEmpty()
    }

    /**
     * When there are no stored contacts it means that there are no connections and all local data is empty, therefore the view that invites
     * to create the first connection should be shown
     * */
    val showNoConnectionsView: LiveData<Boolean> = Transformations.map(totalOfContacts) {
        it == 0
    }
}

/**
 * Factory for [NotificationsViewModel].
 * */
class NotificationsViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}