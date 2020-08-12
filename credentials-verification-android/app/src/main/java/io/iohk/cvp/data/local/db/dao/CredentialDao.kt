package io.iohk.cvp.data.local.db.dao

import androidx.room.*
import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.data.local.db.model.Credential

@Dao
interface CredentialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(credential: Credential): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllCredentials(credentials: List<Credential?>)

    @Query("SELECT * FROM credential order by id desc")
    fun getAllCredentials(): List<Credential>

    @Query("DELETE FROM credential")
    fun removeAllData()

    @Query("SELECT * FROM credential where viewed is 0 order by id asc")
    fun getAllNewCredentials(): List<Credential>

    @Update
    suspend fun updateCredential(credential: Credential)

    @Query("SELECT * FROM credential where credential_id = :credentialId order by id asc")
    suspend fun getCredentialByCredentialId(credentialId : String): Credential?
}