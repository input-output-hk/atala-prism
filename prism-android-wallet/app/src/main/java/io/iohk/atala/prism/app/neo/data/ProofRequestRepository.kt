package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.ProofRequestsLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource

class ProofRequestRepository(
    private val proofRequestsLocalDataSource: ProofRequestsLocalDataSourceInterface,
    private val remoteDataSource: ConnectorRemoteDataSource,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) :
    BaseRepository(
        sessionLocalDataSource,
        preferencesLocalDataSource
    ) {
    fun getAllProofRequest(): LiveData<List<ProofRequest>> = proofRequestsLocalDataSource.allProofRequest()

    suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials? = proofRequestsLocalDataSource.getProofRequestById(id)

    suspend fun removeProofRequest(proofRequest: ProofRequest) = proofRequestsLocalDataSource.removeProofRequest(proofRequest)

    suspend fun declineProofRequest(proofRequest: ProofRequest) {
        removeProofRequest(proofRequest)
    }

    suspend fun acceptProofRequest(id: Long) {
        getProofRequestById(id)?.let { proofRequestData ->
            remoteDataSource.sendCredentialsToContact(proofRequestData.contact!!, proofRequestData.credentials)
            // store activity log
            proofRequestsLocalDataSource.insertRequestedCredentialActivities(proofRequestData.contact!!, proofRequestData.credentials)
            removeProofRequest(proofRequestData.proofRequest)
        } ?: kotlin.run {
            throw Exception("ProofRequest nonexistent")
        }
    }
}
