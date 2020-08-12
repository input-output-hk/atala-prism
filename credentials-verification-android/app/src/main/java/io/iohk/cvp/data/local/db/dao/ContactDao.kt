package io.iohk.cvp.data.local.db.dao

import androidx.room.*
import io.iohk.cvp.data.local.db.model.Contact

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact): Long

    @Query("SELECT * FROM contact order by id asc")
    suspend fun getAll(): List<Contact>

    @Update
    suspend fun updateContact(contact: Contact?)

    @Query("SELECT * FROM contact where connection_id = :connectionId")
    suspend fun getContactByConnectionId(connectionId : String): Contact?

    @Query("DELETE FROM Contact")
    suspend fun removeAllData()
}