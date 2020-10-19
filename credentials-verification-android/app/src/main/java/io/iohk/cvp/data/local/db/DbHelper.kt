package io.iohk.cvp.data.local.db

import androidx.lifecycle.LiveData
import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.data.local.db.model.Credential

interface DbHelper {
    suspend fun saveContact(contact: Contact): Long
    suspend fun getAllContacts(): List<Contact>
    fun allContacts(): LiveData<List<Contact>>
    suspend fun contactById(id: Int): Contact?
    suspend fun saveAllCredentials(credentialsList: List<Credential>)
    suspend fun updateContact(contact: Contact)
    suspend fun getAllCredentials(): List<Credential>
    fun allCredentials(): LiveData<List<Credential>>
    suspend fun getContactByConnectionId(connectionId: String): Contact?
    suspend fun removeAllLocalData()
    suspend fun getAllNewCredentials(): List<Credential>
    suspend fun updateCredential(credential: Credential)
    suspend fun getCredentialByCredentialId(credentialId: String): Credential
    suspend fun deleteCredential(credential: Credential)
    suspend fun deleteContact(contact: Contact)
    suspend fun deleteCredentialByContactId(connectionId: String)
    suspend fun getCredentialsByConnectionId(connectionId: String): List<Credential>
}