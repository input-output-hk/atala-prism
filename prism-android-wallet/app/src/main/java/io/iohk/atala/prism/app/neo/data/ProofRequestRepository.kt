package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.ProofRequestLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource

class ProofRequestRepository(
    private val syncLocalDataSourceInterface: ProofRequestLocalDataSourceInterface,
    private val remoteDataSource: ConnectorRemoteDataSource,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) :
    BaseRepository(
        sessionLocalDataSource,
        preferencesLocalDataSource
    ) {

    fun getAllProofRequest(): LiveData<List<ProofRequest>> = syncLocalDataSourceInterface.allProofRequest()

    suspend fun acceptProofRequest(id: Long) {
        getProofRequestById(id)?.let { proofRequestData ->
            val encodedCredentials = syncLocalDataSourceInterface.loadEncodedCredentials(proofRequestData.credentials)
            remoteDataSource.sendCredentialsToContact(proofRequestData.contact!!, encodedCredentials)
            // store activity log
            syncLocalDataSourceInterface.insertRequestedCredentialActivities(proofRequestData.contact!!, proofRequestData.credentials)
            removeProofRequest(proofRequestData.proofRequest)
        } ?: kotlin.run {
            throw Exception("ProofRequest nonexistent")
        }
    }

    suspend fun getProofRequestById(id: Long): ProofRequestWithContactAndCredentials? = syncLocalDataSourceInterface.getProofRequestById(id)

    suspend fun removeProofRequest(proofRequest: ProofRequest) = syncLocalDataSourceInterface.removeProofRequest(proofRequest)

    suspend fun declineProofRequest(proofRequest: ProofRequest) {
        removeProofRequest(proofRequest)
    }
}
