package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.neo.data.local.ActivityHistoriesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface

class ActivityHistoriesRepository(private val localDataSource: ActivityHistoriesLocalDataSourceInterface,
                                  sessionLocalDataSource: SessionLocalDataSourceInterface,
                                  preferencesLocalDataSource: PreferencesLocalDataSourceInterface) : BaseRepository(sessionLocalDataSource,preferencesLocalDataSource) {

    fun allActivityHistories(): LiveData<List<ActivityHistoryWithContactAndCredential>> = localDataSource.allActivityHistories()


    /*
    * if there are no connections there should be no activity history
    * */
    fun areThereConnections():LiveData<Boolean> = Transformations.map(localDataSource.totalOfContacts()){
        it > 0
    }

    fun allIssuedCredentialsNotifications(): LiveData<List<ActivityHistoryWithCredential>> = localDataSource.allIssuedCredentialsNotifications()
}