package io.iohk.atala.prism.app.ui.payid.step2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.data.PayIdRepository
import io.iohk.atala.prism.app.neo.data.PayIdRepositoryException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class PayIdSetupFormViewModel @Inject constructor(private val repository: PayIdRepository) : ViewModel() {

    companion object {
        const val MAX_PAY_ID_NAME_LENGTH = 50
        const val MIN_PAY_ID_NAME_LENGTH = 4
    }

    sealed class Error {
        object CantLoadCurrentRegisteredPayIdName : Error()
        class PayIdNameAlreadyTaken(val payIdName: String) : Error()
        object TimeOutError : Error()
        class UnknownError(val message: String?) : Error()
    }

    private val _payIdNameIsRegistered = MutableLiveData<Boolean>(false)

    val payIdNameIsRegistered: LiveData<Boolean> = _payIdNameIsRegistered

    val payIdName = MutableLiveData("")

    val walletPublicKey = MutableLiveData("")

    // decides when the continue button should be enabled
    val canContinue = MediatorLiveData<Boolean>().apply {
        addSource(payIdName) { value = computeCanContinue() }
        addSource(walletPublicKey) { value = computeCanContinue() }
        addSource(_payIdNameIsRegistered) { value = computeCanContinue() }
    }

    private val _isLoading = MutableLiveData(false)

    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<EventWrapper<Error>>()

    val error: LiveData<EventWrapper<Error>> = _error

    init {
        // We must be sure that a Pay Id Name is not already registered
        loadRegisteredPayIdName()
    }

    private fun loadRegisteredPayIdName() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val registeredPayIdName = repository.loadCurrentPayIdName()
                payIdName.value = registeredPayIdName
                _payIdNameIsRegistered.value = registeredPayIdName?.isNotBlank() ?: false
            } catch (ex: Exception) {
                _error.value = EventWrapper(Error.CantLoadCurrentRegisteredPayIdName)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun next() {
        if (payIdNameIsRegistered.value != true)
            registerPayId()
        else
            registerWalletAddress()
    }

    private fun registerPayId() {
        payIdName.value?.let { payIdName ->
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    repository.registerPayIdName(payIdName)
                    _payIdNameIsRegistered.value = true
                    _isLoading.value = false
                } catch (ex: PayIdRepositoryException.PayIdNameAlreadyTaken) {
                    ex.printStackTrace()
                    _error.value = EventWrapper(Error.PayIdNameAlreadyTaken(ex.payIdName))
                    _isLoading.value = false
                } catch (ex: TimeoutCancellationException) {
                    _error.value = EventWrapper(Error.TimeOutError)
                    loadRegisteredPayIdName()
                } catch (ex: Exception) {
                    _error.value = EventWrapper(Error.UnknownError(ex.message))
                    loadRegisteredPayIdName()
                }
            }
        }
    }

    private fun registerWalletAddress() {
        walletPublicKey.value?.let { walletPublicKey ->
            // TODO to be implemented
        }
    }

    private fun computeCanContinue(): Boolean {
        val payIdNameLength = payIdName.value?.length ?: 0
        return if (payIdNameIsRegistered.value == true)
            walletPublicKey.value?.isNotBlank() == true
        else
            payIdNameLength in MIN_PAY_ID_NAME_LENGTH..MAX_PAY_ID_NAME_LENGTH
    }
}
