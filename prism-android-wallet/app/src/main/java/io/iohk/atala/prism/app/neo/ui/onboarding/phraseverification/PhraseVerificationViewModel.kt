package io.iohk.atala.prism.app.neo.ui.onboarding.phraseverification

import androidx.lifecycle.*
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.SessionRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class PhraseVerificationViewModel @Inject constructor(private val sessionRepository: SessionRepository) : ViewModel() {

    private lateinit var mnemonicList: List<String>
    var verificationIndex1: Int = -1
    var verificationIndex2: Int = -1

    private val _showInvalidWordsError = MutableLiveData<Boolean>(false)

    val showInvalidWordsError: LiveData<Boolean> = _showInvalidWordsError

    private val expectedWord1: String
        get() = mnemonicList[verificationIndex1].toLowerCase()


    private val expectedWord2: String
        get() = mnemonicList[verificationIndex2].toLowerCase()

    // handle two way binding for the first word TextInputEditText
    val firstWord = MutableLiveData<String>().apply { observeForever { _showInvalidWordsError.value = false } }

    // handle two way binding for the second word TextInputEditText
    val secondWord = MutableLiveData<String>().apply { observeForever { _showInvalidWordsError.value = false } }

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

    fun setArguments(mnemonicList: List<String>, verificationIndex1: Int, verificationIndex2: Int) {
        this.mnemonicList = mnemonicList
        this.verificationIndex1 = verificationIndex1
        this.verificationIndex2 = verificationIndex2
    }

    fun verifyButtonTaped() {
        // Validate words
        if (!expectedWord1.equals(firstWord.value, ignoreCase = true) || !expectedWord2.equals(secondWord.value, ignoreCase = true)) {
            _showInvalidWordsError.value = true
            return
        }
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
        return (firstWord.value ?: "").isNotBlank() && (secondWord.value ?: "").isNotBlank()
    }
}