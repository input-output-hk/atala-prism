package io.iohk.atala.prism.app.data.local.db

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential

interface DbHelper {
    suspend fun getAllContacts(): List<Contact>
    fun allContacts(): LiveData<List<Contact>>
    suspend fun saveContact(contact: Contact): Long
    suspend fun removeAllLocalData()
    suspend fun getContactByConnectionId(connectionId: String): Contact?
    suspend fun insertIssuedCredentialsToAContact(contactId: Long, issuedCredentials: List<Credential>)
    suspend fun updateContact(contact: Contact)
    suspend fun contactById(contactId: Int): Contact?
    suspend fun getAllCredentials(): List<Credential>
    fun allCredentials(): LiveData<List<Credential>>
    suspend fun getCredentialByCredentialId(credentialId: String): Credential?
    suspend fun deleteCredential(credential: Credential)
    suspend fun getCredentialsByConnectionId(connectionId: String): List<Credential>
    suspend fun deleteContact(contact: Contact)
    suspend fun insertShareCredentialActivityHistories(credential: Credential, contacts: List<Contact>)
    suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>)
    suspend fun getCredentialsActivityHistoriesByConnection(connectionId: String): List<ActivityHistoryWithCredential>
}