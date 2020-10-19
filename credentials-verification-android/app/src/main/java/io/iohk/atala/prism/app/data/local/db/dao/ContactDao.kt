package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.iohk.atala.prism.app.data.local.db.model.Contact

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: Contact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<Contact?>)

    @Query("SELECT * FROM contact order by id asc")
    suspend fun getAll(): List<Contact>

    @Query("SELECT * FROM contact order by id asc")
    fun all(): LiveData<List<Contact>>

    @Query("SELECT * from contact where id = :id LIMIT 1")
    suspend fun contactById(id: Int): Contact?

    @Update
    suspend fun updateContact(contact: Contact?)

    @Query("SELECT * FROM contact where connection_id = :connectionId")
    suspend fun getContactByConnectionId(connectionId: String): Contact?

    @Query("DELETE FROM Contact")
    suspend fun removeAllData()

    @Delete
    suspend fun delete(contact: Contact)
}