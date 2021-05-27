package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.neo.data.local.ActivityHistoriesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.model.DashboardNotification

class DashboardRepository(
    private val activityHistoriesLocalDataSource: ActivityHistoriesLocalDataSourceInterface,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) : BaseRepository(sessionLocalDataSource, preferencesLocalDataSource) {

    val totalOfIssuedCredentialsNotifications = activityHistoriesLocalDataSource.totalOfIssuedCredentialsNotifications()

    fun lastActivityHistories(max: Int): LiveData<List<ActivityHistoryWithContactAndCredential>> = activityHistoriesLocalDataSource.lastActivityHistories(max)

    suspend fun getDashboardCardNotifications(): List<DashboardNotification> = preferencesLocalDataSource.getDashboardCardNotifications()

    suspend fun removeDashboardCardNotification(notification: DashboardNotification) = preferencesLocalDataSource.removeDashboardCardNotification(notification)
}
