package io.iohk.atala.prism.app.ui.idverification.step1

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.KycRepository
import javax.inject.Inject

class DocumentScanViewModel @Inject constructor(private val repository: KycRepository) : ViewModel() {

    init {
        repository.restartProcess() // Be sure that when starting this ViewModel there is no Acuant process started
    }

    private val _frontDocumentLoaded = MutableLiveData(false)

    val frontDocumentLoaded: LiveData<Boolean> = _frontDocumentLoaded

    private val _isLoading = MutableLiveData(false)

    val isLoading: LiveData<Boolean> = _isLoading

    private val _showError = MutableLiveData<EventWrapper<KycRepository.KycRepositoryError>>()

    val showError: LiveData<EventWrapper<KycRepository.KycRepositoryError>> = _showError

    private val _shouldGoToNextStep = MutableLiveData(EventWrapper(false))

    val shouldGoToNextStep: LiveData<EventWrapper<Boolean>> = _shouldGoToNextStep

    private val documentVerificationProcessCallBack = object : KycRepository.AcuantDocumentProcessCallBack {

        override fun onSuccess(identityDataFound: Boolean, faceMatchFound: Boolean) {
            if (identityDataFound) {
                // when there is identity data it means that the required images of an
                // identification document have already been scanned (it can be one or two sides)
                // then you can proceed to the next step
                _shouldGoToNextStep.value = EventWrapper(true)
            } else {
                // When there is no identity data yet it means that it is required to scan the back of the document
                _frontDocumentLoaded.value = true
            }
            _isLoading.value = false
        }

        override fun onError(error: KycRepository.KycRepositoryError, processIsRestarted: Boolean) {
            if (processIsRestarted) {
                _frontDocumentLoaded.value = false
            }
            _isLoading.value = false
            _showError.value = EventWrapper(error)
        }
    }

    fun processDocument(croppedDocumentImage: Bitmap, barcodeString: String?) {
        _isLoading.value = true
        repository.handleCapturedDocument(croppedDocumentImage, barcodeString, documentVerificationProcessCallBack)
    }

    fun resetProcess() {
        _frontDocumentLoaded.value = false
        repository.restartProcess()
    }
}
