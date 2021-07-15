package io.iohk.atala.prism.app.ui.idverification.tutorial

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import io.iohk.atala.prism.app.data.local.db.model.KycRequest
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.KycInitializationHelper
import io.iohk.atala.prism.app.neo.data.KycRepository
import javax.inject.Inject

class IdVerificationTutorialViewModel @Inject constructor(private val repository: KycRepository) : ViewModel() {

    private val kycInitializationStatus: LiveData<KycInitializationHelper.KycInitializationResult?> = repository.kycInitializationStatus

    val errorStartingKycFlow: LiveData<EventWrapper<Boolean>> = Transformations.map(kycInitializationStatus) {
        EventWrapper(
            it is KycInitializationHelper.KycInitializationResult.Error ||
                it is KycInitializationHelper.KycInitializationResult.TimeoutError ||
                it is KycInitializationHelper.KycInitializationResult.AcuantError
        )
    }

    val showLoading: LiveData<EventWrapper<Boolean>> = Transformations.map(kycInitializationStatus) {
        EventWrapper(it is KycInitializationHelper.KycInitializationResult.IsLoaDing)
    }

    val acuantSDKIsAlreadyInitialized: LiveData<EventWrapper<KycRequest?>> = Transformations.map(kycInitializationStatus) {
        return@map if (it is KycInitializationHelper.KycInitializationResult.Success) {
            EventWrapper(it.kycRequest)
        } else {
            EventWrapper(null)
        }
    }

    fun startkycInitializationProcess() = repository.initializeAcuantProcess()
}
