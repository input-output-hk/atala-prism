package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.dao.ProofRequestDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncLocalDataSource(private val proofRequestDao: ProofRequestDao, private val contactDao: ContactDao, private val credentialDao: CredentialDao) : SyncLocalDataSourceInterface {

    override fun allProofRequest(): LiveData<List<ProofRequest>> = proofRequestDao.all()

    override suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials? = proofRequestDao.getProofRequestById(id)

    override suspend fun removeProofRequest(proofRequest: ProofRequest) = proofRequestDao.delete(proofRequest)

    override suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>) = withContext(Dispatchers.IO) {
        proofRequestDao.insertRequestedCredentialActivities(contact, credentials)
    }

    override fun allContacts(): LiveData<List<Contact>> = contactDao.all()

    override suspend fun getContactByConnectionId(connectionId: String): Contact? = withContext(Dispatchers.IO) {
        contactDao.getContactByConnectionId(connectionId)
    }

    override suspend fun updateContact(contact: Contact, issuedCredentials: List<Credential>) = withContext(Dispatchers.IO) {
        contactDao.updateContactSync(contact, issuedCredentials)
    }

    override suspend fun credentialsByTypes(credentialTypes: List<String>): List<Credential> = withContext(Dispatchers.IO) {
        credentialDao.getCredentialsByTypes(credentialTypes)
    }

    override suspend fun insertProofRequest(proofRequest: ProofRequest, credentials: List<Credential>): Long = withContext(Dispatchers.IO) {
        proofRequestDao.insertSync(proofRequest, credentials)
    }
}
