package io.iohk.atala.prism.app.ui.main.credentials

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.data.CredentialsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class CredentialDetailViewModel @Inject constructor(
    private val credentialsRepository: CredentialsRepository
) : ViewModel() {

    private val _credentialData = MutableLiveData<CredentialWithEncodedCredential>()

    val credentialData: LiveData<CredentialWithEncodedCredential> = _credentialData

    private val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = credentialsRepository.getCustomDateFormat()
        }
    }

    val receivedDate = MediatorLiveData<String>().apply {
        value = ""
        addSource(customDateFormat) { value = computeCredentialDate() }
        addSource(_credentialData) { value = computeCredentialDate() }
    }

    fun fetchCredentialInfo(credentialId: String) {
        viewModelScope.launch {
            credentialsRepository.getCredentialWithEncodedCredentialByCredentialId(credentialId)?.let {
                _credentialData.postValue(it)
                credentialsRepository.clearCredentialNotifications(it.credential.credentialId)
            }
        }
    }

    private fun computeCredentialDate(): String {
        if (_credentialData.value == null || customDateFormat.value == null) {
            return ""
        }
        return customDateFormat.value!!.dateFormat.format(_credentialData.value!!.credential.dateReceived)
    }
}
