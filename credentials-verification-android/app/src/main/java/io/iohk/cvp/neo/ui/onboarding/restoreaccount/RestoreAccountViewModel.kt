package io.iohk.cvp.neo.ui.onboarding.restoreaccount

import androidx.lifecycle.*
import io.iohk.cvp.core.CvpApplication
import io.iohk.cvp.neo.common.EventWrapper
import io.iohk.cvp.neo.common.exceptions.InvalidSecurityWord
import io.iohk.cvp.neo.common.exceptions.InvalidSecurityWordsLength
import io.iohk.cvp.neo.data.AccountRecoveryRepository
import kotlinx.coroutines.launch

class RestoreAccountViewModel(private val accountRecoveryRepository: AccountRecoveryRepository) : ViewModel() {

    enum class ErrorType { InvalidSecurityWordsLength, InvalidSecurityWord, UnknownError }

    // when the security words change the error messages should disappear in case there is one
    val securityWords = MutableLiveData<List<String>>().apply { observeForever(Observer { _error.value = null }) }

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

/**
 * Factory for [RestoreAccountViewModel].
 * */
class RestoreAccountViewModelFactory() : ViewModelProvider.Factory {

    private val accountRecoveryRepository = CvpApplication.applicationComponent.accountRecoveryRepository()

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RestoreAccountViewModel(accountRecoveryRepository) as T
    }
}