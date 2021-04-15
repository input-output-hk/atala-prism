package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithContact
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential

interface CredentialsLocalDataSourceInterface {

    fun allCredentials(): LiveData<List<Credential>>

    suspend fun clearCredentialNotifications(credentialId: String)

    suspend fun getCredentialByCredentialId(credentialId: String): Credential?

    suspend fun getCredentialWithEncodedCredentialByCredentialId(credentialId: String): CredentialWithEncodedCredential?

    suspend fun getContactsActivityHistoriesByCredentialId(credentialId: String): List<ActivityHistoryWithContact>

    suspend fun deleteCredential(credential: Credential)

    suspend fun contactsToShareCredential(): List<Contact>

    suspend fun insertShareCredentialActivityHistories(
        credential: Credential,
        contacts: List<Contact>
    )
}
