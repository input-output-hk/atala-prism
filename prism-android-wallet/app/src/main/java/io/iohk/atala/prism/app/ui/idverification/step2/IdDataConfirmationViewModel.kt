package io.iohk.atala.prism.app.ui.idverification.step2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.preferences.models.AcuantUserInfo
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.KycRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class IdDataConfirmationViewModel @Inject constructor(private val repository: KycRepository) : ViewModel() {

    private val _acuantUserInfo = MutableLiveData<AcuantUserInfo?>(null)

    val acuantUserInfo: LiveData<AcuantUserInfo?> = _acuantUserInfo

    private val _isLoading = MutableLiveData(false)

    val isLoading: LiveData<Boolean> = _isLoading

    private val _showError = MutableLiveData<EventWrapper<Boolean>>()

    val showError: LiveData<EventWrapper<Boolean>> = _showError

    private val _dataConfirmedCorrectly = MutableLiveData(EventWrapper(false))

    val dataConfirmedCorrectly: LiveData<EventWrapper<Boolean>> = _dataConfirmedCorrectly

    fun loadData() {
        viewModelScope.launch {
            _acuantUserInfo.value = repository.loadAcuantUserInfo()
        }
    }

    fun confirmData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.confirmKycData()
                _dataConfirmedCorrectly.value = EventWrapper(true)
            } catch (ex: Exception) {
                ex.printStackTrace()
                _showError.value = EventWrapper(true)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
