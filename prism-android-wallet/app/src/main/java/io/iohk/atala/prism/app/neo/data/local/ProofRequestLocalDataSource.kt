package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.CredentialDao
import io.iohk.atala.prism.app.data.local.db.dao.ProofRequestDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.EncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProofRequestLocalDataSource(
    private val proofRequestDao: ProofRequestDao,
    private val credentialDao: CredentialDao
) : ProofRequestLocalDataSourceInterface {

    override fun allProofRequest(): LiveData<List<ProofRequest>> = proofRequestDao.all()

    override suspend fun loadEncodedCredentials(credentials: List<Credential>): List<EncodedCredential> = withContext(Dispatchers.IO) {
        // Encoded credentials must be obtained one by one due to the limit of data that exists for a SQLite query
        return@withContext credentials.map {
            credentialDao.getEncodedCredentialByCredentialId(it.credentialId)!!
        }
    }

    override suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>) = withContext(Dispatchers.IO) {
        proofRequestDao.insertRequestedCredentialActivities(contact, credentials)
    }

    override suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials? = proofRequestDao.getProofRequestById(id)

    override suspend fun removeProofRequest(proofRequest: ProofRequest) = proofRequestDao.delete(proofRequest)
}
