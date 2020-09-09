package io.iohk.cvp.neo.ui.onboarding.phraseverification

import androidx.lifecycle.*
import io.iohk.cvp.core.CvpApplication
import io.iohk.cvp.neo.common.EventWrapper
import io.iohk.cvp.neo.data.SessionRepository
import kotlinx.coroutines.launch

class PhraseVerificationViewModel(private val sessionRepository: SessionRepository,
                                  private val mnemonicList: List<String>,
                                  val verificationIndex1: Int,
                                  val verificationIndex2: Int) : ViewModel() {

    private val expectedWord1: String = mnemonicList[verificationIndex1].toLowerCase()

    private val expectedWord2: String = mnemonicList[verificationIndex2].toLowerCase()

    // handle two way binding for the first word TextInputEditText
    val firstWord = MutableLiveData<String>()

    // handle two way binding for the second word TextInputEditText
    val secondWord = MutableLiveData<String>()

    private val _uiEnabled = MutableLiveData<Boolean>().apply { value = true }

    val uiEnabled: LiveData<Boolean> = _uiEnabled

    // Verify Button will be enabled only when both words match
    val verifyButtonEnabled = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(firstWord) { value = computeVerifyButtonEnabled() }
        addSource(secondWord) { value = computeVerifyButtonEnabled() }
        addSource(uiEnabled) { value = computeVerifyButtonEnabled() }
    }

    private val _shouldNavigateToNextScreen = MutableLiveData<EventWrapper<Boolean>>()

    val shouldNavigateToNextScreen: LiveData<EventWrapper<Boolean>> = _shouldNavigateToNextScreen

    fun verifyButtonTaped() {
        // UI is disabled while data is saved
        _uiEnabled.value = false
        viewModelScope.launch {
            // store the data
            sessionRepository.storeSession(mnemonicList)
            // re-enable the UI
            _uiEnabled.value = true
            _shouldNavigateToNextScreen.value = EventWrapper(true)
        }
    }

    private fun computeVerifyButtonEnabled(): Boolean {
        if (_uiEnabled.value == false) {
            return false
        }
        val word1 = (firstWord.value ?: "").toLowerCase()
        val word2 = (secondWord.value ?: "").toLowerCase()
        return word1.contentEquals(expectedWord1) && word2.contentEquals(expectedWord2)
    }
}

/**
 * Factory for [PhraseVerificationViewModel].
 * */
class PhraseVerificationViewModelFactory(private val mnemonicList: List<String>,
                                         private val verificationIndex1: Int,
                                         private val verificationIndex2: Int) : ViewModelProvider.Factory {

    private val sessionRepository = CvpApplication.applicationComponent.sessionRepository()

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PhraseVerificationViewModel(sessionRepository, mnemonicList,
                verificationIndex1,
                verificationIndex2) as T
    }
}