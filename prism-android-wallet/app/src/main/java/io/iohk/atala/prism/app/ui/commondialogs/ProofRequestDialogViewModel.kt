package io.iohk.atala.prism.app.ui.commondialogs

import androidx.lifecycle.*
import com.google.protobuf.ByteString
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.stream.Collectors
import javax.inject.Inject

class ProofRequestDialogViewModel @Inject constructor(private val dataManager: DataManager) : ViewModel() {

    private var proofRequest: ProofRequest? = null

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
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getProofRequestById(proofRequestId)?.let { proofRequestWhitCredentials ->
                proofRequest = proofRequestWhitCredentials.proofRequest
                val contact = dataManager.getContactByConnectionId(proofRequestWhitCredentials.proofRequest.connectionId)
                _contact.postValue(contact)
                val checkableCredentials = proofRequestWhitCredentials.credentials.map {
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
        _contact.value?.let { contact ->
            _showLoading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                // TODO this logic requires an appropriate repository
                _proofRequestAccepted.postValue(EventWrapper(false))
                dataManager.removeProofRequest(proofRequest!!)
                _showLoading.postValue(false)
            }
        }
    }

    fun acceptProofRequest() {
        _contact.value?.let { contact ->
            _showLoading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                // TODO this logic requires an appropriate repository
                val credentials = _requestedCredentials.value!!.map { it.data }
                val messages: List<ByteString> = credentials.stream().map {
                    it.credentialEncoded
                }.collect(Collectors.toList())
                val keyPath = dataManager.getKeyPairFromPath(contact.keyDerivationPath)
                try {
                    dataManager.sendMultipleMessage(keyPath, contact.connectionId, messages)
                    dataManager.insertRequestedCredentialActivities(contact, credentials)
                    dataManager.removeProofRequest(proofRequest!!)
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