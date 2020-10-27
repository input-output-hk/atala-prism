package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.grpc.AsyncTaskResult
import io.iohk.atala.prism.app.views.utils.SingleLiveEvent
import kotlinx.coroutines.*
import java.lang.Exception
import javax.inject.Inject

/*
* TODO This ViewModel is being used by 3 independent views [CredentialDetailFragment], [DeleteCredentialDialogFragment],
*  and [HomeFragment] it is necessary to split this into 3 independent ViewModel´s.
**/
class CredentialsViewModel @Inject constructor(val dataManager: DataManager) : NewConnectionsViewModel(dataManager) {

    private val _credentials = MutableLiveData(AsyncTaskResult<List<Credential>>())

    private val _credentialDeletedLiveData = MutableLiveData<Boolean>(false)

    private val _showErrorMessageLiveData = SingleLiveEvent<Boolean>()

    // TODO This got broken, it will be fixed in the notification screen refactorization
    fun getNewCredentials() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                //val credentialsList = dataManager.getAllNewCredentials()
                _credentials.postValue(AsyncTaskResult(listOf()))
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

    // TODO This got broken, it will be fixed in the notification screen refactorization
    fun setCredentialViewed(credential: Credential) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                //credential.viewed = true
                //dataManager.updateCredential(credential)
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
                val credential = dataManager.getCredentialByCredentialId(credentialId)!!
                dataManager.deleteCredential(credential)
                _credentialDeletedLiveData.postValue(true)
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                _showErrorMessageLiveData.postValue(true)
            }
        }
    }
}
