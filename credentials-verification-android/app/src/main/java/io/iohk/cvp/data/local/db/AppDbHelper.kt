package io.iohk.cvp.data.local.db

import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.data.local.db.model.Credential
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDbHelper @Inject constructor(private val mAppDatabase: AppDatabase) : DbHelper {
    override suspend fun saveContact(contact: Contact): Long {
        return mAppDatabase.contactDao().insert(contact)
    }

    override suspend fun getAllContacts(): List<Contact> {
        return mAppDatabase.contactDao().getAll()
    }

    override suspend fun saveAllCredentials(credentialsList:List<Credential>) {
        mAppDatabase.credentialDao().insertAllCredentials(credentialsList)
    }

    override suspend fun updateContact(contact: Contact) {
        mAppDatabase.contactDao().updateContact(contact)
    }

    override suspend fun getAllCredentials(): List<Credential> {
        return mAppDatabase.credentialDao().getAllCredentials()
    }

    override suspend fun getContactByConnectionId(connectionId: String): Contact? {
        return mAppDatabase.contactDao().getContactByConnectionId(connectionId)
    }

    override suspend fun removeAllLocalData() {
        mAppDatabase.contactDao().removeAllData()
        mAppDatabase.credentialDao().removeAllData()
    }

    override suspend fun getAllNewCredentials(): List<Credential> {
        return mAppDatabase.credentialDao().getAllNewCredentials()
    }

    override suspend fun updateCredential(credential: Credential) {
        mAppDatabase.credentialDao().updateCredential(credential)
    }

    override suspend fun getCredentialByCredentialId(credentialId: String): Credential? {
        return mAppDatabase.credentialDao().getCredentialByCredentialId(credentialId)
    }
}
