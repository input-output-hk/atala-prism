package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
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
}