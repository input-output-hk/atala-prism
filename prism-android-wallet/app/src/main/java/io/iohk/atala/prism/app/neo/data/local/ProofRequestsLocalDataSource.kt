package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.dao.ProofRequestDao
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProofRequestsLocalDataSource(private val proofRequestDao: ProofRequestDao) : ProofRequestsLocalDataSourceInterface {

    override fun allProofRequest(): LiveData<List<ProofRequest>> = proofRequestDao.all()

    override suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials? = proofRequestDao.getProofRequestById(id)

    override suspend fun removeProofRequest(proofRequest: ProofRequest) = proofRequestDao.delete(proofRequest)

    override suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>) = withContext(Dispatchers.IO) {
        proofRequestDao.insertRequestedCredentialActivities(contact, credentials)
    }
}