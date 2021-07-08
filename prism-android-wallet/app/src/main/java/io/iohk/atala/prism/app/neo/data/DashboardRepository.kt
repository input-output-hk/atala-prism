package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.neo.data.local.ActivityHistoriesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PayIdLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.model.DashboardNotification

class DashboardRepository(
    private val payIdLocalDataSource: PayIdLocalDataSourceInterface,
    private val activityHistoriesLocalDataSource: ActivityHistoriesLocalDataSourceInterface,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) : BaseRepository(sessionLocalDataSource, preferencesLocalDataSource) {

    private val preferencesDashboardCardNotifications: LiveData<List<DashboardNotification>> = preferencesLocalDataSource.dashboardCardNotifications()

    private val payId: LiveData<PayId?> = payIdLocalDataSource.getPayIdByStatusLiveData(PayId.Status.Registered)

    val dashboardCardNotifications = MediatorLiveData<List<DashboardNotification>>().apply {
        addSource(preferencesDashboardCardNotifications) { value = computeDashboardCardNotifications() }
        addSource(payId) { value = computeDashboardCardNotifications() }
    }

    private fun computeDashboardCardNotifications(): List<DashboardNotification> {
        var result = preferencesDashboardCardNotifications.value?.toMutableList() ?: mutableListOf()
        payId.value?.let { _ ->
            result.removeIf { it == DashboardNotification.PayId }
        }
        return result
    }

    val totalOfIssuedCredentialsNotifications = activityHistoriesLocalDataSource.totalOfIssuedCredentialsNotifications()

    fun lastActivityHistories(max: Int): LiveData<List<ActivityHistoryWithContactAndCredential>> = activityHistoriesLocalDataSource.lastActivityHistories(max)

    suspend fun removeDashboardCardNotification(notification: DashboardNotification) = preferencesLocalDataSource.removeDashboardCardNotification(notification)
}
