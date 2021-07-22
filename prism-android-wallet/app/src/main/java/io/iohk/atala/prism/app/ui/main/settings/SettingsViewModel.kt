package io.iohk.atala.prism.app.ui.main.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.grpc.AsyncTaskResult
import io.iohk.atala.prism.app.neo.data.PreferencesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel @Inject constructor(private val repository: PreferencesRepository) : ViewModel() {

    private val _removeAllDataLiveData = MutableLiveData<AsyncTaskResult<Boolean>>()

    val thereIsAPayId: LiveData<Boolean> = Transformations.map(repository.payId) {
        it != null
    }

    fun removeAllLocalData() {
        viewModelScope.launch {
            try {
                repository.resetData()
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
