package io.iohk.atala.prism.app.data.local.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistory
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import java.io.InvalidObjectException
import java.util.*

@Dao
abstract class ContactDao : ActivityHistoryDao() {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun rawInsert(contact: Contact): Long

    /**
     * Insert a contact with the necessary [ActivityHistory] inside a transaction.
     *
     * @param contact [Contact]
     * @return returns the generated id [Long] of the created contact.
     */
    @Transaction
    open suspend fun insert(contact: Contact): Long {
        val contactId = rawInsert(contact)
        insertActivityHistory(
                ActivityHistory(
                        contact.connectionId,
                        null,
                        contact.dateCreated,
                        ActivityHistory.Type.ContactAdded
                )
        )
        return contactId
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun rawInsertAll(contacts: List<Contact?>): List<Long>

    /**
     * Insert a list of contacts with the necessary [ActivityHistory] inside a transaction.
     *
     * @param contacts [List<Contact>]
     * @return returns the generated id´s [List] of [Long] the created contacts.
     */
    @Transaction
    open suspend fun insertAll(contacts: List<Contact>): List<Long> {
        val ids = rawInsertAll(contacts)
        val activityHistories: List<ActivityHistory> = contacts.map {
            ActivityHistory(it.connectionId, null, it.dateCreated, ActivityHistory.Type.ContactAdded)
        }
        insertActivityHistories(activityHistories)
        return ids
    }

    /**
     * Find all the soft deleted contacts.
     *
     * @return returns all the deleted contacts [List] of [Contact].
     */
    @Query("SELECT * FROM contacts WHERE deleted = 1 order by id asc")
    abstract suspend fun getAllRemoved(): List<Contact>

    /**
     * Find all non-deleted contacts
     *
     * @return All non-deleted contacts [List] of [Contact].
     */
    @Query("SELECT * FROM contacts WHERE deleted = 0 order by id asc")
    abstract suspend fun getAll(): List<Contact>

    /**
     * Make a [LiveData] where the results of all contacts not deleted are reflected
     *
     * @return a [LiveData] where the results of all contacts not deleted are reflected.
     */
    @Query("SELECT * FROM contacts WHERE deleted = 0 order by id asc")
    abstract fun all(): LiveData<List<Contact>>

    /**
     * Find a [Contact] by :id
     *
     * @return a @nullable [Contact] .
     */
    @Query("SELECT * from contacts where id = :id LIMIT 1")
    abstract suspend fun contactById(id: Int): Contact?

    /**
     * Find a [Contact] by :connection_id
     *
     * @return a @nullable [Contact].
     */
    @Query("SELECT * FROM contacts where connection_id = :connectionId")
    abstract suspend fun getContactByConnectionId(connectionId: String): Contact?

    /**
     * Update a [Contact]
     */
    @Update
    abstract suspend fun updateContact(contact: Contact?)

    /**
     * Completely delete all [Contact] records and in turn delete all [Credential] data and [ActivityHistory] by cascading.
     * */
    @Query("DELETE FROM contacts")
    abstract suspend fun removeAllData()

    /**
     * Delete a [Contact] with the necessary [ActivityHistory] inside a transaction.
     *
     * @param contact [Contact]
     * */
    @Transaction
    open suspend fun delete(contact: Contact) {
        contact.deleted = true
        updateContact(contact)
        deleteContactCredentials(contact.connectionId)
        val activityHistory = ActivityHistory(contact.connectionId, null, Date().time, ActivityHistory.Type.ContactDeleted)
        insertActivityHistory(activityHistory)
    }

    /**
     * Soft erase all [Credential] issued by a [Contact].
     *
     * @param connectionId [String]
     */
    @Query("UPDATE credentials set deleted = 1 WHERE connection_id = :connectionId")
    abstract suspend fun deleteContactCredentials(connectionId: String)

    /**
     * Find all [Credential] issued by a [Contact].
     *
     * @param connectionId [String] of the contact
     */
    @Query("SELECT * FROM credentials WHERE connection_id = :connectionId")
    abstract suspend fun getIssuedCredentials(connectionId: String): List<Credential>

    /**
     * Insert a [Contact] and their issued [Credential]´s with the necessary [ActivityHistory] inside a transaction.
     *
     * @throws <InvalidObjectException>, @InvalidObjectException when trying to insert a credential with different connection id than the contact
     * @param contact [Contact]
     * @param issuedCredentials [List] of [Credential]
     * @return returns the generated id [Long] of the created contact.
     */
    @Transaction
    open suspend fun insert(contact: Contact, issuedCredentials: List<Credential>): Long {
        issuedCredentials.find {
            it.connectionId != contact.connectionId
        }?.let {
            throw InvalidObjectException("The contact and the credential issued must have the same connectionId")
        }
        val contactId = insert(contact)
        insertCredentials(issuedCredentials)
        val activityHistories: List<ActivityHistory> = issuedCredentials.map {
            ActivityHistory(contact.connectionId, it.credentialId, it.dateReceived, ActivityHistory.Type.CredentialIssued)
        }
        insertActivityHistories(activityHistories)
        return contactId
    }

    /**
     * Insert a list of [Credential]´s issued by an existing [Contact] inside a transaction.
     *
     * @throws <InvalidObjectException>, @InvalidObjectException when trying to insert a credential with different connection id than the contact
     * @param contactId [Long]
     * @param issuedCredentials [List] of [Credential]
     */
    @Transaction
    open suspend fun insertIssuedCredentialsToAContact(contactId: Long, issuedCredentials: List<Credential>) {
        val contact = contactById(contactId.toInt())!!
        issuedCredentials.find {
            it.connectionId != contact.connectionId
        }?.let {
            throw InvalidObjectException("The contact and the credential issued must have the same connectionId")
        }
        insertCredentials(issuedCredentials)
        val activityHistories: List<ActivityHistory> = issuedCredentials.map {
            ActivityHistory(contact.connectionId, it.credentialId, it.dateReceived, ActivityHistory.Type.CredentialIssued)
        }
        insertActivityHistories(activityHistories)
    }

    /**
     * Insert a batch of [Contact]´s with their [Credential]´s
     *
     * @throws <InvalidObjectException>, @InvalidObjectException when trying to insert a credential with different connection id than the contact
     * @param contactsWithIssuedCredentials [Map<Contact, List<Credential>>]
     */
    @Transaction
    open suspend fun insertAll(contactsWithIssuedCredentials: Map<Contact, List<Credential>>) {
        contactsWithIssuedCredentials.forEach { (contact, credentials) ->
            insert(contact, credentials)
        }
    }

    /**
     * Find all [ActivityHistory]´s related to a [Contact]  and that these are related to a credential in other words find all issued, shared and requested [Credential]´s of a [Contact]
     *
     * @param connectionId [String] of the Contact
     * @return a [List] of [ActivityHistoryWithCredential]
     */
    @Query("SELECT * FROM activityHistories WHERE connection_id = :connectionId AND credential_id IS NOT NULL ORDER BY date asc")
    abstract suspend fun getCredentialsActivityHistoriesByConnection(connectionId: String): List<ActivityHistoryWithCredential>

    /**
     * Find all [ActivityHistory] related to a [Contact] in other words find all issued, shared and requested [Credential]´s and all deleteContact or createContact [ActivityHistory] of a [Contact]
     *
     * @param connectionId [String] of the Contact
     * @return a [List] of [ActivityHistoryWithCredential]
     */
    @Query("SELECT * FROM activityHistories WHERE connection_id = :connectionId ORDER BY date asc")
    abstract suspend fun getActivityHistoriesByConnection(connectionId: String): List<ActivityHistoryWithCredential>

    /**
     * Inserts [ActivityHistory]´s of requests for [Credential]´s from a [Contact]
     *
     * @param contact [Contact] the contact that makes the requests.
     * @param credentials [List] of [Credential] the credentials requested.
     * */
    @Transaction
    open suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>) {
        val activitiesHistories = credentials.map {
            ActivityHistory(contact.connectionId, it.credentialId, Date().time, ActivityHistory.Type.CredentialRequested)
        }
        insertActivityHistories(activitiesHistories)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCredentials(credentials: List<Credential>): List<Long>
}