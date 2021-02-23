package io.iohk.atala.prism.app.ui.main.credentials

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.data.CredentialsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class CredentialDetailViewModel @Inject constructor(
    private val credentialsRepository: CredentialsRepository
) : ViewModel() {

    private val _credential = MutableLiveData<Credential>()

    val credential: LiveData<Credential> = _credential

    private val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = credentialsRepository.getCustomDateFormat()
        }
    }

    val receivedDate = MediatorLiveData<String>().apply {
        value = ""
        addSource(customDateFormat) { value = computeCredentialDate() }
        addSource(_credential) { value = computeCredentialDate() }
    }

    fun fetchCredentialInfo(credentialId: String) {
        viewModelScope.launch {
            credentialsRepository.getCredentialByCredentialId(credentialId)?.let {
                _credential.postValue(it)
                credentialsRepository.clearCredentialNotifications(it.credentialId)
            }
        }
    }

    private fun computeCredentialDate(): String {
        if (_credential.value == null || customDateFormat.value == null) {
            return ""
        }
        return customDateFormat.value!!.dateFormat.format(_credential.value!!.dateReceived)
    }
}
