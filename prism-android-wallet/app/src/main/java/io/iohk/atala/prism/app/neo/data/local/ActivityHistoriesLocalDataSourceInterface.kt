package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential

interface ActivityHistoriesLocalDataSourceInterface {

    fun allActivityHistories(): LiveData<List<ActivityHistoryWithContactAndCredential>>

    fun totalOfContacts(): LiveData<Int>

    fun allIssuedCredentialsNotifications(): LiveData<List<ActivityHistoryWithCredential>>
}