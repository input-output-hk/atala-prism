package io.iohk.atala.prism.app.ui.idverification.step1

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.iohk.atala.prism.app.neo.common.EventWrapper

class IdTypeSelectionViewModel : ViewModel() {

    enum class IdType { NationalId, Passport, DriverLicense }

    private val _selectedIdType = MutableLiveData<IdType?>()

    val selectedIdType: LiveData<IdType?> = _selectedIdType

    private val _shouldContinue = MutableLiveData<EventWrapper<IdType?>>()

    val shouldContinue: LiveData<EventWrapper<IdType?>> = _shouldContinue

    fun selectIdType(idType: IdType) {
        _selectedIdType.value = idType
    }

    fun next() {
        selectedIdType.value?.let {
            _shouldContinue.value = EventWrapper(it)
        }
    }
}
