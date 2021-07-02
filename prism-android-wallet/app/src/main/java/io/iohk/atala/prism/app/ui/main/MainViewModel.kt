package io.iohk.atala.prism.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.ProofRequestRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(private val repository: ProofRequestRepository) : ViewModel() {

    private val _securityViewShouldBeVisible = MutableLiveData<EventWrapper<Boolean>>()

    val securityViewShouldBeVisible: LiveData<EventWrapper<Boolean>> = _securityViewShouldBeVisible

    private val proofRequests: LiveData<List<ProofRequest>> = repository.getAllProofRequest()

    val proofRequest: LiveData<EventWrapper<ProofRequest>> = Transformations.map(proofRequests) {
        return@map if (it.isNotEmpty()) EventWrapper(it[0]) else null
    }

    /*
    * Verify if there is any security pin assigned, if so it should show the security view
    * */
    fun checkSecuritySettings() {
        viewModelScope.launch {
            _securityViewShouldBeVisible.postValue(EventWrapper(repository.isSecurityPinConfigured()))
        }
    }
}
