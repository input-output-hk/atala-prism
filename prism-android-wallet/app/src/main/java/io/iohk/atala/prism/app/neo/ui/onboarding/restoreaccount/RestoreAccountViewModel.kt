package io.iohk.atala.prism.app.neo.ui.onboarding.restoreaccount

import androidx.lifecycle.*
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.exceptions.InvalidSecurityWord
import io.iohk.atala.prism.app.neo.common.exceptions.InvalidSecurityWordsLength
import io.iohk.atala.prism.app.neo.data.AccountRecoveryRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class RestoreAccountViewModel @Inject constructor(private val accountRecoveryRepository: AccountRecoveryRepository) : ViewModel() {

    enum class ErrorType { InvalidSecurityWordsLength, InvalidSecurityWord, UnknownError }

    // when the security words change the error messages should disappear in case there is one
    val securityWords = MutableLiveData<List<String>>().apply { observeForever(Observer { _error.value = null }) }

    val acceptButtonEnabled = Transformations.map(securityWords) {
        return@map it.size == 12
    }

    private val _error = MutableLiveData<ErrorType>()

    val error: LiveData<ErrorType> = _error

    private val _isLoading = MutableLiveData<Boolean>()

    val isLoading: LiveData<Boolean> = _isLoading

    private val _accountRestoredSuccessfully = MutableLiveData<EventWrapper<Boolean>>()

    val accountRestoredSuccessfully: LiveData<EventWrapper<Boolean>> = _accountRestoredSuccessfully

    fun sendPhrase() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountRecoveryRepository.recoverAccount(securityWords.value ?: listOf())
                _accountRestoredSuccessfully.value = EventWrapper(true)
            } catch (ex: InvalidSecurityWordsLength) {
                _error.value = ErrorType.InvalidSecurityWordsLength
            } catch (ex: InvalidSecurityWord) {
                _error.value = ErrorType.InvalidSecurityWord
            } catch (ex: Exception) {
                _error.value = ErrorType.UnknownError
            } finally {
                _isLoading.value = false
            }
        }
    }
}