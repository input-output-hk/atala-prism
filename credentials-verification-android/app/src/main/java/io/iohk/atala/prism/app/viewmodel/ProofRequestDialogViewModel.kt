package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import com.google.protobuf.ByteString
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.stream.Collectors

class ProofRequestDialogViewModel(private val dataManager: DataManager) : ViewModel() {

    // id of the proof request message
    private lateinit var messageId: String

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

    fun fetchProofRequestInfo(messageId: String, connectionId: String, requestedCredentialsIds: List<String>) {
        this.messageId = messageId
        _showLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val contact = dataManager.getContactByConnectionId(connectionId)
            _contact.postValue(contact)
            val checkableCredentials = dataManager.getCredentialsByCredentialsIds(requestedCredentialsIds).map {
                CheckableData(it)
            }
            _requestedCredentials.postValue(checkableCredentials)
            _showLoading.postValue(false)
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
                // the message with the proof request is set as a seen
                contact.lastMessageId = messageId
                dataManager.updateContact(contact)
                _proofRequestAccepted.postValue(EventWrapper(false))
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

/**
 * Factory for [ProofRequestDialogViewModel].
 * */
class ProofRequestDialogViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}