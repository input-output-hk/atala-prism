package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.data.local.CredentialsLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource

class CredentialsRepository(
        private val credentialsLocalDataSource: CredentialsLocalDataSourceInterface,
        private val remoteDataSource: ConnectorRemoteDataSource,
        sessionLocalDataSource: SessionLocalDataSourceInterface,
        preferencesLocalDataSource: PreferencesLocalDataSourceInterface)
    : BaseRepository(
        sessionLocalDataSource,
        preferencesLocalDataSource
) {

    fun allCredentials(): LiveData<List<Credential>> = credentialsLocalDataSource.allCredentials()

    suspend fun clearCredentialNotifications(credentialId: String) = credentialsLocalDataSource.clearCredentialNotifications(credentialId)

    suspend fun getCredentialByCredentialId(credentialId: String): Credential? = credentialsLocalDataSource.getCredentialByCredentialId(credentialId)

    suspend fun getContactsActivityHistoriesByCredentialId(credentialId: String): List<ActivityHistoryWithContact> = credentialsLocalDataSource.getContactsActivityHistoriesByCredentialId(credentialId)

    suspend fun deleteCredential(credential: Credential) = credentialsLocalDataSource.deleteCredential(credential)

    suspend fun contactsToShareCredential(credential: Credential): List<Contact> {
        return credentialsLocalDataSource.contactsToShareCredential(credential)
    }

    suspend fun shareCredential(credential: Credential, contacts: List<Contact>) {
        remoteDataSource.sendCredentialToMultipleContacts(credential, contacts)
        credentialsLocalDataSource.insertShareCredentialActivityHistories(credential, contacts)
    }
}