package io.iohk.atala.prism.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.iohk.atala.prism.app.data.local.db.model.CredentialHistory

@Dao
interface CredentialHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(credentialHistory: CredentialHistory): Long

    @Query("SELECT * FROM credential_history  ORDER BY id ASC")
    fun getAll(): List<CredentialHistory>
}
