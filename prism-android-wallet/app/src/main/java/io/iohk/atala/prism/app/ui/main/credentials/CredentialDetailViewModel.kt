package io.iohk.atala.prism.app.ui.main.credentials

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import kotlinx.coroutines.*
import javax.inject.Inject

/*
* TODO This ViewModel is being used by 3 independent views [CredentialDetailFragment], [DeleteCredentialDialogFragment],
*  and [HomeFragment] it is necessary to split this into 3 independent ViewModel´s.
**/
class CredentialDetailViewModel @Inject constructor(private val dataManager: DataManager) : ViewModel() {

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
}