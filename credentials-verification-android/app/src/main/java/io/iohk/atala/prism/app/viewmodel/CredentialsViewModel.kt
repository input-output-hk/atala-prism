package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
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

    private val _credentialDeletedLiveData = MutableLiveData<Boolean>(false)

    private val _showErrorMessageLiveData = SingleLiveEvent<Boolean>()

    fun setCredentialViewed(credential: Credential) {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.clearCredentialNotifications(credential.credentialId)
        }
    }

    /*
    * TODO this is for [CredentialDetailFragment] delete this when there is an appropriate ViewModel for that fragment
    * */
    val customDateFormat = MutableLiveData<CustomDateFormat>().apply {
        viewModelScope.launch {
            value = dataManager.getCurrentDateFormat()
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
