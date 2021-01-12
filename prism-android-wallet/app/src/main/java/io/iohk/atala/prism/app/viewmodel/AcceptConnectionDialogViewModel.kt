package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.*
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.grpc.ParticipantInfoResponse
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.protos.ParticipantInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AcceptConnectionDialogViewModel(private val dataManager: DataManager) : ViewModel() {

    private var token: String? = null

    private val _participantInfoResponse = MutableLiveData<ParticipantInfoResponse>()

    val participantInfoResponse: LiveData<ParticipantInfoResponse> = _participantInfoResponse

    val participantName: LiveData<String> = Transformations.map(participantInfoResponse) {
        if (it.participantInfo.participantCase.number == ParticipantInfo.ParticipantCase.ISSUER.number)
            it.participantInfo.issuer.name
        else
            it.participantInfo.verifier.name
    }

    val participantLogo: LiveData<ByteArray> = Transformations.map(participantInfoResponse) {
        if (it.participantInfo.participantCase.number == ParticipantInfo.ParticipantCase.ISSUER.number)
            it.participantInfo.issuer.logo.toByteArray()
        else
            it.participantInfo.verifier.logo.toByteArray()
    }

    private val _isLoading = MutableLiveData<Boolean>(false)

    val isLoading: LiveData<Boolean> = _isLoading

    private val _connectionIsConfirmed = MutableLiveData<EventWrapper<Boolean>>()

    val connectionIsConfirmed: LiveData<EventWrapper<Boolean>> = _connectionIsConfirmed

    private val _connectionError = MutableLiveData<EventWrapper<Boolean>>()

    val connectionError: LiveData<EventWrapper<Boolean>> = _connectionError

    private val _connectionIsDeclined = MutableLiveData<EventWrapper<Boolean>>()

    val connectionIsDeclined: LiveData<EventWrapper<Boolean>> = _connectionIsDeclined


    fun fetchConnectionTokenInfo(token: String) {
        this.token = token
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                // TODO - move this logic into a repository including logic to know if we are already connected to that connection
                val participantInfoResponse = dataManager.getConnectionTokenInfo(token)
                val connectionsList = dataManager.getAllContacts()
                val found = connectionsList.any { connection: Contact ->
                    connection.name == participantInfoResponse.creator.issuer.name || connection.name == participantInfoResponse.creator.holder.name
                }
                _participantInfoResponse.postValue(ParticipantInfoResponse(participantInfoResponse.creator, token, found))
            } catch (ex: Exception) {
                _connectionError.postValue(EventWrapper(true))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun confirm() {
        token?.let {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    _isLoading.postValue(true)
                    // TODO - move this logic into a repository, it could be something like repository.confirmConnection(tokenString)
                    val currentIndex = dataManager.getCurrentIndex()
                    val addConnectionFromTokenResponse = dataManager.addConnection(dataManager.getKeyPairFromPath(CryptoUtils.getNextPathFromIndex(currentIndex)), it, "")
                    dataManager.saveContact(ContactMapper.mapToContact(addConnectionFromTokenResponse.connection, CryptoUtils.getNextPathFromIndex(currentIndex)))
                    dataManager.increaseIndex()
                    //_newConnectionInfoLiveData.postValue(AsyncTaskResult(addConnectionFromTokenResponse))
                    _connectionIsConfirmed.postValue(EventWrapper(true))
                } catch (ex: Exception) {
                    _connectionError.postValue(EventWrapper(true))
                }
            }
        }
    }

    fun decline() {
        // TODO For now this does nothing, we must know what we will have to do with it
        _connectionIsDeclined.postValue(EventWrapper(true))
    }
}

/**
 * Factory for [AcceptConnectionDialogViewModel].
 * */
class AcceptConnectionDialogViewModelFactory(private val dataManager: DataManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DataManager::class.java).newInstance(dataManager)
    }
}