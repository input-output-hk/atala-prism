package io.iohk.cvp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import io.iohk.cvp.data.DataManager
import io.iohk.cvp.data.local.db.model.Credential
import io.iohk.cvp.grpc.AsyncTaskResult
import kotlinx.coroutines.*
import java.lang.Exception
import javax.inject.Inject

class CredentialsViewModel @Inject constructor(val dataManager: DataManager) : NewConnectionsViewModel(dataManager) {

    private val _credentials = MutableLiveData(AsyncTaskResult<List<Credential>>())

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

    fun getCredentialLiveData(): MutableLiveData<AsyncTaskResult<List<Credential>>> {
        return _credentials
    }

    fun setCredentialViewed(credential: Credential) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                credential.viewed = true
                dataManager.updateCredential(credential)
            } catch (ex: Exception) {
                Crashlytics.logException(ex)
            }
        }
    }
}
