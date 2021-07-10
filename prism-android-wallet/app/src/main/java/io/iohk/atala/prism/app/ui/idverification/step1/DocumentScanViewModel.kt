package io.iohk.atala.prism.app.ui.idverification.step1

import android.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acuant.acuantcommon.model.Error
import com.acuant.acuantcommon.type.CardSide
import com.acuant.acuantdocumentprocessing.model.IDResult
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.KycRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class DocumentScanViewModel @Inject constructor(private val repository: KycRepository) : ViewModel() {

    private val _frontDocumentResult = MutableLiveData<IDResult?>()

    val frontDocumentResult: LiveData<IDResult?> = _frontDocumentResult

    private val _backDocumentResult = MutableLiveData<IDResult?>()

    val backDocumentResult: LiveData<IDResult?> = _backDocumentResult

    private val _isLoading = MutableLiveData(false)

    val isLoading: LiveData<Boolean> = _isLoading

    private val _showError = MutableLiveData<EventWrapper<Pair<Boolean, Error?>>>()

    val showError: LiveData<EventWrapper<Pair<Boolean, Error?>>> = _showError

    private val _shouldGoNextStep = MutableLiveData<EventWrapper<IDResult>>()

    val shouldGoNextStep: LiveData<EventWrapper<IDResult>> = _shouldGoNextStep

    private val documentVerificationProcessCallBack = object : KycRepository.AcuantDocumentProcessCallBack {
        override fun onSuccess(result: IDResult, side: CardSide) {
            when (side) {
                CardSide.Front -> _frontDocumentResult.value = result
                CardSide.Back -> {
                    _backDocumentResult.value = result
                    _shouldGoNextStep.value = EventWrapper(frontDocumentResult.value!!)
                }
            }
            _isLoading.value = false
        }

        override fun onError(error: Error?) {
            _isLoading.value = false
            _showError.value = EventWrapper(Pair(true, error))
        }
    }

    fun processDocument(croppedDocumentImageUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            if (frontDocumentResult.value != null) {
                repository.continueVerificationProcessWithDocumentBackSide(
                    croppedDocumentImageUrl,
                    documentVerificationProcessCallBack
                )
            } else {
                repository.startVerificationProcessWithDocumentFrontSide(
                    croppedDocumentImageUrl,
                    documentVerificationProcessCallBack
                )
            }
        }
    }

    fun resetProcess() {
        _frontDocumentResult.value = null
    }
}
