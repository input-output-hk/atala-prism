package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CredentialsLocalDataSource(private val credentialDao: CredentialDao) : CredentialsLocalDataSourceInterface {

    override fun allCredentials(): LiveData<List<Credential>> = credentialDao.all()

    override suspend fun clearCredentialNotifications(credentialId: String) {
        return withContext(Dispatchers.IO) {
            credentialDao.clearCredentialNotifications(credentialId)
        }
    }

    override suspend fun getCredentialByCredentialId(credentialId: String): Credential? {
        return withContext(Dispatchers.IO) {
            return@withContext credentialDao.getCredentialByCredentialId(credentialId)
        }
    }

    override suspend fun getCredentialWithEncodedCredentialByCredentialId(credentialId: String): CredentialWithEncodedCredential? = withContext(Dispatchers.IO) {
        return@withContext credentialDao.getCredentialWithEncodedCredentialByCredentialId(credentialId)
    }

    override suspend fun getContactsActivityHistoriesByCredentialId(credentialId: String): List<ActivityHistoryWithContact> {
        return withContext(Dispatchers.IO) {
            return@withContext credentialDao.getContactsActivityHistoriesByCredentialId(credentialId)
        }
    }

    override suspend fun deleteCredential(credential: Credential) {
        return withContext(Dispatchers.IO) {
            credentialDao.delete(credential)
        }
    }

    override suspend fun contactsToShareCredential(): List<Contact> {
        return withContext(Dispatchers.IO) {
            return@withContext credentialDao.contactsToShareCredential()
        }
    }

    override suspend fun insertShareCredentialActivityHistories(
        credential: Credential,
        contacts: List<Contact>
    ) {
        return withContext(Dispatchers.IO) {
            credentialDao.insertShareCredentialActivityHistories(credential, contacts)
        }
    }

    override suspend fun credentialsByTypes(credentialTypes: List<String>): List<Credential> = withContext(Dispatchers.IO) {
        credentialDao.getCredentialsByTypes(credentialTypes)
    }

    override suspend fun credentialsByExcludedTypes(excludedTypes: List<String>): List<Credential> = withContext(Dispatchers.IO) {
        credentialDao.getCredentialsByExcludedTypes(excludedTypes)
    }
}
