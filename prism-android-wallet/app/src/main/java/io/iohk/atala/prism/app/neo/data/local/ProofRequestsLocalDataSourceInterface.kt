package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials

interface ProofRequestsLocalDataSourceInterface {

    fun allProofRequest(): LiveData<List<ProofRequest>>

    suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials?

    suspend fun removeProofRequest(proofRequest: ProofRequest)

    suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>)
}
