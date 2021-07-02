package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.ContactDao
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.dao.ProofRequestDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConnectorListenerLocalDataSource(
    private val proofRequestDao: ProofRequestDao,
    private val contactDao: ContactDao,
    private val credentialDao: CredentialDao
) : ConnectorListenerLocalDataSourceInterface {

    override fun allContacts(): LiveData<List<Contact>> = contactDao.all()

    override suspend fun getContactByConnectionId(connectionId: String): Contact? = withContext(Dispatchers.IO) {
        contactDao.getContactByConnectionId(connectionId)
    }

    override suspend fun updateContact(contact: Contact, issuedCredentials: List<CredentialWithEncodedCredential>) = withContext(Dispatchers.IO) {
        contactDao.updateContactSync(contact, issuedCredentials)
    }

    override suspend fun credentialsByTypes(credentialTypes: List<String>): List<Credential> = withContext(Dispatchers.IO) {
        credentialDao.getCredentialsByTypes(credentialTypes)
    }

    override suspend fun insertProofRequest(proofRequest: ProofRequest, credentials: List<Credential>): Long = withContext(Dispatchers.IO) {
        proofRequestDao.insertSync(proofRequest, credentials)
    }
}
