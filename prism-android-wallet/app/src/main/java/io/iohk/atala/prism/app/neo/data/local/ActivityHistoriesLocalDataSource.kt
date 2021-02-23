package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential

class ActivityHistoriesLocalDataSource(private val contactDao: ContactDao) : ActivityHistoriesLocalDataSourceInterface {

    override fun allActivityHistories(): LiveData<List<ActivityHistoryWithContactAndCredential>> = contactDao.activityHistories()

    override fun totalOfContacts(): LiveData<Int> = contactDao.totalOfContacts()

    override fun allIssuedCredentialsNotifications(): LiveData<List<ActivityHistoryWithCredential>> = contactDao.allIssuedCredentialsNotifications()
}
