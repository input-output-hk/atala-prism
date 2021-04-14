package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.EncodedCredential
import java.util.Date

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

    @Query("SELECT * FROM credentials where credential_id = :credentialId order by id asc")
    abstract suspend fun getCredentialWithEncodedCredentialByCredentialId(credentialId: String): CredentialWithEncodedCredential?

    @Query("SELECT * FROM encodedCredentials where credential_id = :credentialId order by id asc")
    abstract suspend fun getEncodedCredentialByCredentialId(credentialId: String): EncodedCredential?

    @Query("SELECT * FROM credentials WHERE credential_id IN (:credentialsIds)")
    abstract suspend fun getCredentialsByCredentialsIds(credentialsIds: List<String>): List<Credential>

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

    @Query("UPDATE activityHistories set needs_to_be_notified = 0 WHERE credential_id = :credentialId")
    abstract suspend fun clearCredentialNotifications(credentialId: String)

    /**
     * Returns a list of contacts to whom a credential can be shared
     * *
     * @param credentialConnectionId [String] connectionId of the Credential
     * @return a [List] of [Contact]
     */
    @Query("SELECT * FROM contacts WHERE deleted = 0 order by id asc")
    abstract suspend fun contactsToShareCredential(): List<Contact>

    @Query("SELECT * FROM credentials WHERE credential_type IN (:credentialTypes) order by id asc")
    abstract suspend fun getCredentialsByTypes(credentialTypes: List<String>): List<Credential>

    @Query("SELECT COUNT(*) FROM encodedCredentials ORDER BY id asc")
    abstract suspend fun totalOfEncodedCredentials(): Int
}
