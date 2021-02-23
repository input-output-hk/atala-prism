package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContactAndCredential
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import java.util.Date

@Dao
abstract class ActivityHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertActivityHistory(activityHistory: ActivityHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertActivityHistories(activityHistories: List<ActivityHistory>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertActivityHistoriesSync(activityHistories: List<ActivityHistory>): List<Long>

    @Query("SELECT * FROM activityHistories ORDER BY id ASC")
    abstract suspend fun getAllActivityHistories(): List<ActivityHistory>

    @Query("SELECT * FROM activityHistories ORDER BY date DESC, id")
    abstract fun activityHistories(): LiveData<List<ActivityHistoryWithContactAndCredential>>

    /**
     * Inserts [ActivityHistory]´s of requests for [Credential]´s from a [Contact]
     *
     * @param contact [Contact] the contact that makes the requests.
     * @param credentials [List] of [Credential] the credentials requested.
     * */
    @Transaction
    open suspend fun insertRequestedCredentialActivities(
        contact: Contact,
        credentials: List<Credential>
    ) {
        val activitiesHistories = credentials.map {
            ActivityHistory(contact.connectionId, it.credentialId, Date().time, ActivityHistory.Type.CredentialRequested)
        }
        insertActivityHistories(activitiesHistories)
    }

    @Query("SELECT * FROM activityHistories WHERE needs_to_be_notified = 1 AND credential_id IS NOT NULL AND type = :type ORDER BY date asc, id")
    abstract fun allIssuedCredentialsNotifications(
        type: Int = ActivityHistory.Type.CredentialIssued.value
    ): LiveData<List<ActivityHistoryWithCredential>>
}
