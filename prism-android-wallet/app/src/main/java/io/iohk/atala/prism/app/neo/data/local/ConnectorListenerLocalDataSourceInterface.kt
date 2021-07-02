package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest

interface ConnectorListenerLocalDataSourceInterface {

    fun allContacts(): LiveData<List<Contact>>

    suspend fun getContactByConnectionId(connectionId: String): Contact?

    suspend fun updateContact(contact: Contact, issuedCredentials: List<CredentialWithEncodedCredential>)

    suspend fun credentialsByTypes(credentialTypes: List<String>): List<Credential>

    suspend fun insertProofRequest(proofRequest: ProofRequest, credentials: List<Credential>): Long
}
