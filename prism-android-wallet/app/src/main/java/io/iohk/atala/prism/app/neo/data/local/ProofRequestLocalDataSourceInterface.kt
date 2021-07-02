package io.iohk.atala.prism.app.neo.data.local

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.EncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials

interface ProofRequestLocalDataSourceInterface {

    fun allProofRequest(): LiveData<List<ProofRequest>>

    suspend fun loadEncodedCredentials(credentials: List<Credential>): List<EncodedCredential>

    suspend fun insertRequestedCredentialActivities(contact: Contact, credentials: List<Credential>)

    suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials?

    suspend fun removeProofRequest(proofRequest: ProofRequest)
}
