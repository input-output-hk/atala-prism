package io.iohk.atala.prism.app.data.local.db

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDbHelper @Inject constructor(private val mAppDatabase: AppDatabase) : DbHelper {
    override suspend fun getAllContacts(): List<Contact> {
        return mAppDatabase.contactDao().getAll()
    }

    override fun allContacts(): LiveData<List<Contact>> {
        return mAppDatabase.contactDao().all()
    }

    override suspend fun saveContact(contact: Contact): Long {
        return mAppDatabase.contactDao().insert(contact)
    }

    override suspend fun removeAllLocalData() {
        // This is a permanent deletion in the contacts entity, and should also cascade the entire credentials entity and activityHistories.
        mAppDatabase.contactDao().removeAllData()
    }

    override suspend fun getContactByConnectionId(connectionId: String): Contact? {
        return mAppDatabase.contactDao().getContactByConnectionId(connectionId)
    }

    override suspend fun insertIssuedCredentialsToAContact(contactId: Long, issuedCredentials: List<Credential>) {
        mAppDatabase.contactDao().insertIssuedCredentialsToAContact(contactId, issuedCredentials)
    }

    override suspend fun updateContact(contact: Contact) {
        mAppDatabase.contactDao().updateContact(contact)
    }

    override suspend fun contactById(contactId: Int): Contact? {
        return mAppDatabase.contactDao().contactById(contactId)
    }

    override suspend fun getAllCredentials(): List<Credential> {
        return mAppDatabase.credentialDao().getAllCredentials()
    }

    override fun allCredentials(): LiveData<List<Credential>> {
        return mAppDatabase.credentialDao().all()
    }

    override suspend fun getCredentialByCredentialId(credentialId: String): Credential? {
        return mAppDatabase.credentialDao().getCredentialByCredentialId(credentialId)
    }

    override suspend fun deleteCredential(credential: Credential) {
        mAppDatabase.credentialDao().delete(credential)
    }

    override suspend fun getCredentialsByConnectionId(connectionId: String): List<Credential> {
        return mAppDatabase.contactDao().getIssuedCredentials(connectionId)
    }

    override suspend fun deleteContact(contact: Contact) {
        mAppDatabase.contactDao().delete(contact)
    }

    override suspend fun insertShareCredentialActivityHistories(credential: Credential, contacts: List<Contact>) {
        mAppDatabase.credentialDao().insertShareCredentialActivityHistories(credential, contacts)
    }

    override suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>) {
        mAppDatabase.contactDao().insertRequestedCredentialActivities(contact, credentials)
    }

    override suspend fun getCredentialsActivityHistoriesByConnection(connectionId: String): List<ActivityHistoryWithCredential> {
        return mAppDatabase.contactDao().getCredentialsActivityHistoriesByConnection(connectionId)
    }

    override suspend fun getContactsActivityHistoriesByCredentialId(credentialId: String): List<ActivityHistoryWithContact> {
        return mAppDatabase.credentialDao().getContactsActivityHistoriesByCredentialId(credentialId)
    }
}