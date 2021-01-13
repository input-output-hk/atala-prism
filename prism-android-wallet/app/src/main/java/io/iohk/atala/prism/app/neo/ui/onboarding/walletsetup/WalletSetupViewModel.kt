package io.iohk.atala.prism.app.neo.ui.onboarding.walletsetup

import androidx.lifecycle.*
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.extensions.randomIndexes
import io.iohk.atala.prism.app.neo.data.SessionRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class WalletSetupViewModel @Inject constructor(sessionRepository: SessionRepository) : ViewModel() {

    companion object {
        private val MNEMONIC_LIST_PLACE_HOLDER = (1..12).map { "------" }
    }

    private val _mnemonicList = MutableLiveData<List<String>>().apply {
        viewModelScope.launch {
            val mnemonicList = sessionRepository.getNewMnemonicList()
            // select random indexes for two words for user verification
            userVerificationWordsIndexes = mnemonicList.randomIndexes().take(2)
            value = mnemonicList
        }
    }

    // exposed mnemonic list to be renderer
    val mnemonicList = MediatorLiveData<List<String>>().apply {
        // initial value is a place holder
        value = MNEMONIC_LIST_PLACE_HOLDER
        // handle when _mnemonicList is ready to be rendered
        addSource(_mnemonicList) { value = _mnemonicList.value ?: MNEMONIC_LIST_PLACE_HOLDER }
    }

    // user need accept the current mnemonic list
    val mnemonicListAccepted = MutableLiveData<Boolean>().apply { value = false }

    // list of indexes of two words to be verified by the user on the next screen
    lateinit var userVerificationWordsIndexes: List<Int>
        private set

    // accept button can be enabled when exist a mnemonic list and the user accept this list
    val acceptButtonEnabled = MediatorLiveData<Boolean>().apply {
        addSource(_mnemonicList) { value = computeAcceptButtonEnabled() }
        addSource(mnemonicListAccepted) { value = computeAcceptButtonEnabled() }
    }

    private val _shouldGoToNextScreen = MutableLiveData<EventWrapper<List<String>>>()

    val shouldGoToNextScreen: LiveData<EventWrapper<List<String>>> = _shouldGoToNextScreen

    fun acceptButtonTaped() {
        _mnemonicList.value?.let {
            _shouldGoToNextScreen.value = EventWrapper(it)
        }
    }

    private fun computeAcceptButtonEnabled(): Boolean {
        return _mnemonicList.value != null && mnemonicListAccepted.value == true
    }
}