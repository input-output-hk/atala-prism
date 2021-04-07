package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsLocalDataSource(private val contactDao: ContactDao) : ContactsLocalDataSourceInterface {
    override suspend fun storeContactsWithIssuedCredentials(
        contactsWithIssuedCredentials: Map<Contact, List<CredentialWithEncodedCredential>>
    ) {
        contactDao.insertAll(contactsWithIssuedCredentials)
    }

    override suspend fun getContactById(contactId: Int): Contact? {
        return withContext(Dispatchers.IO) {
            return@withContext contactDao.contactById(contactId)
        }
    }

    override suspend fun getCredentialsActivityHistories(connectionId: String): List<ActivityHistoryWithCredential> {
        return withContext(Dispatchers.IO) {
            return@withContext contactDao.getCredentialsActivityHistoriesByConnection(connectionId)
        }
    }

    override fun allContacts(): LiveData<List<Contact>> = contactDao.all()

    override suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        contactDao.getAll()
    }

    override suspend fun getIssuedCredentials(contactConnectionId: String): List<Credential> {
        return withContext(Dispatchers.IO) {
            return@withContext contactDao.getIssuedCredentials(contactConnectionId)
        }
    }

    override suspend fun deleteContact(contact: Contact) = withContext(Dispatchers.IO) { contactDao.delete(contact) }

    override suspend fun storeContact(contact: Contact) {
        return withContext(Dispatchers.IO) {
            contactDao.insert(contact)
        }
    }

    override suspend fun removeAllContacts() = withContext(Dispatchers.IO) {
        contactDao.removeAllData()
    }
}
