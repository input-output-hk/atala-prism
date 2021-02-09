package io.iohk.atala.prism.app.ui.commondialogs

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequestWithContactAndCredentials
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import io.iohk.atala.prism.app.neo.data.ProofRequestRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProofRequestDialogViewModel @Inject constructor(private val repository: ProofRequestRepository) : ViewModel() {

    private var proofRequestData: ProofRequestWithContactAndCredentials? = null

    private val _contact = MutableLiveData<Contact>()

    val contact: LiveData<Contact> = _contact

    private val _requestedCredentials = MutableLiveData<List<CheckableData<Credential>>>()

    val requestedCredentials: LiveData<List<CheckableData<Credential>>> = _requestedCredentials

    private val _showLoading = MutableLiveData<Boolean>(false)

    val showLoading: LiveData<Boolean> = _showLoading

    private val _proofRequestAccepted = MutableLiveData<EventWrapper<Boolean>>()

    val proofRequestAccepted: LiveData<EventWrapper<Boolean>> = _proofRequestAccepted

    val acceptButtonEnabled: LiveData<Boolean> = Transformations.map(_requestedCredentials) {
        it.find { !it.isChecked } == null
    }

    private val _showError = MutableLiveData<EventWrapper<Boolean>>()

    val showError: LiveData<EventWrapper<Boolean>> = _showError

    fun fetchProofRequestInfo(proofRequestId: Long) {
        _showLoading.value = true
        viewModelScope.launch {
            repository.getProofRequestById(proofRequestId)?.let { proofRequestWhitContactAndCredentials ->
                proofRequestData = proofRequestWhitContactAndCredentials
                _contact.postValue(proofRequestWhitContactAndCredentials.contact)
                val checkableCredentials = proofRequestWhitContactAndCredentials.credentials.map {
                    CheckableData(it)
                }
                _requestedCredentials.postValue(checkableCredentials)
                _showLoading.postValue(false)
            } ?: kotlin.run {

            }
        }
    }

    fun switchCredentialSelection(credential: Credential) {
        val list = _requestedCredentials.value?.toList() ?: listOf()
        val item = list.find { it.data == credential }
        item?.setChecked(!item.isChecked)
        _requestedCredentials.value = list
    }

    fun declineProofRequest() {
        proofRequestData?.let {
            _showLoading.value = true
            viewModelScope.launch {
                repository.declineProofRequest(it.proofRequest)
                _showLoading.postValue(false)
            }
        }
    }

    fun acceptProofRequest() {
        proofRequestData?.let {
            _showLoading.value = true
            viewModelScope.launch {
                try {
                    repository.acceptProofRequest(it.proofRequest.id)
                    _proofRequestAccepted.postValue(EventWrapper(true))
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    _showError.postValue(EventWrapper(true))
                } finally {
                    _showLoading.postValue(false)
                }
            }
        }
    }
}