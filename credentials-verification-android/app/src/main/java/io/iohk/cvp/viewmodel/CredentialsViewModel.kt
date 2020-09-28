package io.iohk.cvp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.iohk.cvp.data.DataManager
import io.iohk.cvp.data.local.db.model.Credential
import io.iohk.cvp.grpc.AsyncTaskResult
import io.iohk.cvp.views.utils.SingleLiveEvent
import kotlinx.coroutines.*
import java.lang.Exception
import javax.inject.Inject

class CredentialsViewModel @Inject constructor(val dataManager: DataManager) : NewConnectionsViewModel(dataManager) {

    private val _credentials = MutableLiveData(AsyncTaskResult<List<Credential>>())

    private val _credentialDeletedLiveData = MutableLiveData<Boolean>(false)

    private val _showErrorMessageLiveData = SingleLiveEvent<Boolean>()

    fun getNewCredentials() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val credentialsList = dataManager.getAllNewCredentials()
                _credentials.postValue(AsyncTaskResult(credentialsList))
            } catch (ex: Exception) {
                _credentials.postValue(AsyncTaskResult(ex))
            }
        }
    }

    fun getAllCredentials() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val credentialsList = dataManager.getAllCredentials()
                _credentials.postValue(AsyncTaskResult(credentialsList))
            } catch (ex: Exception) {
                _credentials.postValue(AsyncTaskResult(ex))
            }
        }
    }

    fun getCredentialLiveData(): LiveData<AsyncTaskResult<List<Credential>>> {
        return _credentials
    }

    fun setCredentialViewed(credential: Credential) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                credential.viewed = true
                dataManager.updateCredential(credential)
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
            }
        }
    }

    fun getDeleteCredentialLiveData(): LiveData<Boolean> {
        return _credentialDeletedLiveData;
    }

    fun getShowErrorMessageLiveData(): SingleLiveEvent<Boolean> {
        return _showErrorMessageLiveData;
    }

    fun deleteCredential(credentialId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val credential = dataManager.getCredentialByCredentialId(credentialId)
                dataManager.deleteCredential(credential)
                _credentialDeletedLiveData.postValue(true)
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                _showErrorMessageLiveData.postValue(true)
            }
        }
    }
}
