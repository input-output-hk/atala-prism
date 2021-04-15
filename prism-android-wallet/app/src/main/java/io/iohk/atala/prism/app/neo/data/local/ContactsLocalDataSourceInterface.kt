package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential

interface ContactsLocalDataSourceInterface {

    suspend fun storeContactsWithIssuedCredentials(
        contactsWithIssuedCredentials: Map<Contact, List<CredentialWithEncodedCredential>>
    )

    suspend fun getContactById(contactId: Int): Contact?

    suspend fun getCredentialsActivityHistories(connectionId: String): List<ActivityHistoryWithCredential>

    fun allContacts(): LiveData<List<Contact>>

    suspend fun getAllContacts(): List<Contact>

    suspend fun getIssuedCredentials(contactConnectionId: String): List<Credential>

    suspend fun deleteContact(contact: Contact)

    suspend fun storeContact(contact: Contact)

    suspend fun removeAllContacts()
}
