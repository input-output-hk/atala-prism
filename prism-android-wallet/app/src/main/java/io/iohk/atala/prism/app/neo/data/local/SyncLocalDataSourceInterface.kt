package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials

interface SyncLocalDataSourceInterface {

    fun allProofRequest(): LiveData<List<ProofRequest>>

    suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials?

    suspend fun removeProofRequest(proofRequest: ProofRequest)

    suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>)

    fun allContacts(): LiveData<List<Contact>>

    suspend fun getContactByConnectionId(connectionId: String): Contact?

    suspend fun updateContact(contact: Contact, issuedCredentials: List<Credential>)

    suspend fun credentialsByTypes(credentialTypes: List<String>): List<Credential>

    suspend fun insertProofRequest(proofRequest: ProofRequest, credentials: List<Credential>): Long
}
