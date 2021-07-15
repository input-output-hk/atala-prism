package io.iohk.atala.prism.app.ui.idverification.step2

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.KycRepository
import javax.inject.Inject

class IdSelfieViewModel @Inject constructor(private val repository: KycRepository) : ViewModel() {

    private val _isLoading = MutableLiveData(false)

    val isLoading: LiveData<Boolean> = _isLoading

    private val _faceMatchingSuccess = MutableLiveData(EventWrapper(false))

    val faceMatchingSuccess: LiveData<EventWrapper<Boolean>> = _faceMatchingSuccess

    private val _faceMatchingFailed = MutableLiveData(EventWrapper(false))

    val faceMatchingFailed: LiveData<EventWrapper<Boolean>> = _faceMatchingFailed

    private val documentVerificationProcessCallBack = object : KycRepository.AcuantDocumentProcessCallBack {

        override fun onSuccess(identityDataFound: Boolean, faceMatchFound: Boolean) {
            if (identityDataFound && faceMatchFound) {
                _faceMatchingSuccess.postValue(EventWrapper(true))
            }
            _isLoading.postValue(false)
        }

        override fun onError(error: KycRepository.KycRepositoryError, processIsRestarted: Boolean) {
            _isLoading.postValue(false)
            _faceMatchingFailed.postValue(EventWrapper(true))
        }
    }

    fun processSelfie(image: Bitmap) {
        _isLoading.value = true
        repository.handleSelfieImage(image, documentVerificationProcessCallBack)
    }
}
