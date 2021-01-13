package io.iohk.atala.prism.app.ui.main.settings

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.grpc.AsyncTaskResult
import kotlinx.coroutines.*
import javax.inject.Inject

class SettingsViewModel @Inject constructor(private val dataManager: DataManager) : ViewModel() {

    private val _removeAllDataLiveData = MutableLiveData<AsyncTaskResult<Boolean>>()

    fun removeAllLocalData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataManager.removeAllLocalData()
                _removeAllDataLiveData.postValue(AsyncTaskResult(true))
            } catch (ex: Exception) {
                _removeAllDataLiveData.postValue(AsyncTaskResult(ex))
            }
        }
    }

    fun getRemoveAllDataLiveData(): MutableLiveData<AsyncTaskResult<Boolean>> {
        return _removeAllDataLiveData
    }
}