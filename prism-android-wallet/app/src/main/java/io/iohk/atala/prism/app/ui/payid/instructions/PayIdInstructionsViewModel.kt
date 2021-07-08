package io.iohk.atala.prism.app.ui.payid.instructions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.neo.data.PayIdRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class PayIdInstructionsViewModel @Inject constructor(payIdRepository: PayIdRepository) : ViewModel() {

    private val _shouldGoToPayIdDetail = MutableLiveData(false)

    val shouldGoToPayIdDetail: LiveData<Boolean> = _shouldGoToPayIdDetail

    init {
        viewModelScope.launch {
            if (payIdRepository.loadCurrentPayId() != null) {
                _shouldGoToPayIdDetail.value = true
            }
        }
    }
}
