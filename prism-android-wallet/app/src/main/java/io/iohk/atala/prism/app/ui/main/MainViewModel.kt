package io.iohk.atala.prism.app.ui.main

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithCredentials
import io.iohk.atala.prism.app.neo.common.EventWrapper
import kotlinx.coroutines.delay
import javax.inject.Inject

class MainViewModel @Inject constructor(private val dataManager: DataManager) : ViewModel() {

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
}