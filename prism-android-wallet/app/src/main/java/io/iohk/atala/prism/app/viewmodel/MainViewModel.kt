package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithCredentials
import io.iohk.atala.prism.app.grpc.AsyncTaskResult
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.protos.AddConnectionFromTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(private val dataManager: DataManager) : NewConnectionsViewModel(dataManager) {

    companion object {
        private const val SYNC_TIME_IN_MILLISECONDS = 7000L
    }

    val requestSync: LiveData<EventWrapper<Boolean>> = liveData {
        while (true) {
            emit(EventWrapper(true))
            delay(SYNC_TIME_IN_MILLISECONDS)
        }
    }

    private val proofRequests: LiveData<List<ProofRequestWithCredentials>> = dataManager.allProofRequest()

    val proofRequest: LiveData<EventWrapper<ProofRequestWithCredentials>> = Transformations.map(proofRequests) {
        return@map if (it.isNotEmpty()) EventWrapper(it[0]) else null
    }

    private val _newConnectionInfoLiveData = MutableLiveData<AsyncTaskResult<AddConnectionFromTokenResponse>>()

    fun addConnectionFromToken(token: String, nonce: String)
            : LiveData<AsyncTaskResult<AddConnectionFromTokenResponse>>? {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentIndex = dataManager.getCurrentIndex()
                val addConnectionFromTokenResponse = dataManager.addConnection(dataManager.getKeyPairFromPath(CryptoUtils.getNextPathFromIndex(currentIndex)), token, nonce)
                dataManager.saveContact(ContactMapper.mapToContact(addConnectionFromTokenResponse.connection, CryptoUtils.getNextPathFromIndex(currentIndex)))
                dataManager.increaseIndex()
                _newConnectionInfoLiveData.postValue(AsyncTaskResult(addConnectionFromTokenResponse))
            } catch (ex: Exception) {
                _newConnectionInfoLiveData.postValue(AsyncTaskResult(ex))
            }
        }
        return _newConnectionInfoLiveData
    }
}