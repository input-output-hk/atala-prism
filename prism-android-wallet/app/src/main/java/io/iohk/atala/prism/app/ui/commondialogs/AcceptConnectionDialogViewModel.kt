package io.iohk.atala.prism.app.ui.commondialogs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.grpc.ParticipantInfoResponse
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.ContactsRepository
import io.iohk.atala.prism.protos.ParticipantInfo
import kotlinx.coroutines.launch
import javax.inject.Inject

class AcceptConnectionDialogViewModel @Inject constructor(
    private val repository: ContactsRepository
) : ViewModel() {

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
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                _participantInfoResponse.postValue(repository.getParticipantInfoResponse(token))
            } catch (ex: Exception) {
                _connectionError.postValue(EventWrapper(true))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun confirm() {
        token?.let { token ->
            viewModelScope.launch {
                try {
                    _isLoading.postValue(true)
                    repository.acceptConnection(token)
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
