package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import java.util.*

@Dao
abstract class CredentialDao : ActivityHistoryDao() {

    @Query("SELECT * FROM credentials WHERE deleted = 1 order by id asc")
    abstract fun getAllDeletedCredentials(): List<Credential>

    @Query("SELECT * FROM credentials WHERE deleted = 0 order by id asc")
    abstract fun getAllCredentials(): List<Credential>

    @Query("SELECT * FROM credentials WHERE deleted = 0 order by id asc")
    abstract fun all(): LiveData<List<Credential>>

    @Query("SELECT * FROM credentials where credential_id = :credentialId order by id asc")
    abstract suspend fun getCredentialByCredentialId(credentialId: String): Credential?

    @Update
    abstract suspend fun updateCredential(credential: Credential)

    @Transaction
    open suspend fun delete(credential: Credential) {
        credential.deleted = true
        updateCredential(credential)
        val activityHistory = ActivityHistory(null, credential.credentialId, Date().time, ActivityHistory.Type.CredentialDeleted)
        insertActivityHistory(activityHistory)
    }

    @Transaction
    open suspend fun insertShareCredentialActivityHistories(credential: Credential, contacts: List<Contact>) {
        val activitiesHistories = contacts.map {
            ActivityHistory(it.connectionId, credential.credentialId, Date().time, ActivityHistory.Type.CredentialShared)
        }
        insertActivityHistories(activitiesHistories)
    }

    /**
     * Find all [ActivityHistory]´s related to a [Credential]  and that these are related to a [Contact] in other words find all issued, shared and requested activities of a [Credential]
     *
     * @param credentialId [String] of the Credential
     * @return a [List] of [ActivityHistoryWithContact]
     */
    @Query("SELECT * FROM activityHistories WHERE credential_id = :credentialId AND connection_id IS NOT NULL ORDER BY date asc, id")
    abstract suspend fun getContactsActivityHistoriesByCredentialId(credentialId: String): List<ActivityHistoryWithContact>

    @Query("SELECT * FROM activityHistories WHERE needs_to_be_notified = 1 AND credential_id IS NOT NULL AND type = :type ORDER BY date asc, id")
    abstract fun allIssuedCredentialsNotifications(type: Int = ActivityHistory.Type.CredentialIssued.value): LiveData<List<ActivityHistoryWithCredential>>

    @Query("UPDATE activityHistories set needs_to_be_notified = 0 WHERE credential_id = :credentialId")
    abstract suspend fun clearCredentialNotifications(credentialId: String)
}