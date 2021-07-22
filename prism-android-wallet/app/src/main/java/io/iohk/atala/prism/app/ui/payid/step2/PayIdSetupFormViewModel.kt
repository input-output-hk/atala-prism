package io.iohk.atala.prism.app.ui.payid.step2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.softCardanoAddressValidation
import io.iohk.atala.prism.app.neo.common.softCardanoExtendedPublicKeyValidation
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
        class PayIdNameAlreadyTaken(val payIdName: String) : Error()
        object TimeOutError : Error()
        class ServerError(val message: String?) : Error()
        class UnknownError(val message: String?) : Error()
    }

    private val _payId = MutableLiveData<PayId?>(null)

    val payIdName = MutableLiveData("")

    val walletPublicKey = MutableLiveData("")

    // decides when the continue button should be enabled
    val canContinue = MediatorLiveData<Boolean>().apply {
        addSource(payIdName) { value = computeCanContinue() }
        addSource(walletPublicKey) { value = computeCanContinue() }
        addSource(_payId) { value = computeCanContinue() }
    }

    private val _isLoading = MutableLiveData(false)

    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<EventWrapper<Error>>()

    val error: LiveData<EventWrapper<Error>> = _error

    private val _showNameConfirmationDialog = MutableLiveData<EventWrapper<Boolean>>()

    val showNameConfirmationDialog: LiveData<EventWrapper<Boolean>> = _showNameConfirmationDialog

    private val totalOfPayIdAddresses: LiveData<Int> = repository.totalOfPayIdAddresses

    private val totalOfPayIdPublicKeys: LiveData<Int> = repository.totalOfPayIdPublicKeys

    val eventRegistrationIsCompleted = MediatorLiveData<EventWrapper<Boolean>>().apply {
        addSource(totalOfPayIdAddresses) { value = EventWrapper(computeEventRegistrationIsCompleted()) }
        addSource(totalOfPayIdPublicKeys) { value = EventWrapper(computeEventRegistrationIsCompleted()) }
        addSource(_payId) { value = EventWrapper(computeEventRegistrationIsCompleted()) }
    }

    val payIdIsRegistered = Transformations.map(_payId) {
        it != null
    }

    init {
        // We must be sure that a Pay Id Name is not already registered
        loadRegisteredPayId()
    }

    private fun loadRegisteredPayId() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _payId.value = repository.loadCurrentPayId()
                payIdName.value = _payId.value?.name
            } catch (ex: TimeoutCancellationException) {
                ex.printStackTrace()
                _error.value = EventWrapper(Error.TimeOutError)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun next() {
        if (_payId.value == null)
            checkPayIdNameAvailability()
        else
            registerWalletAddress()
    }

    private fun checkPayIdNameAvailability() {
        payIdName.value?.let { payIdName ->
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    val nameAvailability = repository.checkPayIdNameAvailability(payIdName)
                    if (nameAvailability) {
                        _showNameConfirmationDialog.postValue(EventWrapper(true))
                    } else {
                        _error.value = EventWrapper(Error.PayIdNameAlreadyTaken(payIdName))
                    }
                } catch (ex: PayIdRepositoryException.AtalaError) {
                    ex.printStackTrace()
                    _error.value = EventWrapper(Error.ServerError(ex.message))
                } catch (ex: PayIdRepositoryException.PayIdAlreadyRegistered) {
                    ex.printStackTrace()
                    loadRegisteredPayId()
                } catch (ex: TimeoutCancellationException) {
                    ex.printStackTrace()
                    _error.value = EventWrapper(Error.TimeOutError)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    _error.value = EventWrapper(Error.UnknownError(ex.message))
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun confirmPayIdNameRegistration() {
        payIdName.value?.let { payIdName ->
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    repository.registerPayIdName(payIdName)?.let {
                        _payId.value = it
                        this@PayIdSetupFormViewModel.payIdName.value = it.name
                    }
                    _isLoading.value = false
                } catch (ex: PayIdRepositoryException.PayIdNameAlreadyTaken) {
                    ex.printStackTrace()
                    _error.value = EventWrapper(Error.PayIdNameAlreadyTaken(ex.payIdName))
                } catch (ex: PayIdRepositoryException.AtalaError) {
                    ex.printStackTrace()
                    _error.value = EventWrapper(Error.ServerError(ex.message))
                } catch (ex: TimeoutCancellationException) {
                    ex.printStackTrace()
                    _error.value = EventWrapper(Error.TimeOutError)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    _error.value = EventWrapper(Error.UnknownError(ex.message))
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun registerWalletAddress() {
        walletPublicKey.value?.let { walletPublicKey ->
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    repository.registerAddressOrPublicKey(walletPublicKey)
                    _isLoading.value = false
                } catch (ex: TimeoutCancellationException) {
                    _error.value = EventWrapper(Error.TimeOutError)
                } catch (ex: PayIdRepositoryException.AtalaError) {
                    _error.value = EventWrapper(Error.ServerError(ex.message))
                } catch (ex: Exception) {
                    _error.value = EventWrapper(Error.UnknownError(ex.message))
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    private fun computeCanContinue(): Boolean {
        val payIdNameLength = payIdName.value?.length ?: 0
        return if (_payId.value != null)
        // if a Pay Id already exists, the cardano address is validated
            softCardanoAddressValidation(walletPublicKey.value ?: "") || softCardanoExtendedPublicKeyValidation(walletPublicKey.value ?: "")
        else
            payIdNameLength in MIN_PAY_ID_NAME_LENGTH..MAX_PAY_ID_NAME_LENGTH
    }

    private fun computeEventRegistrationIsCompleted(): Boolean {
        val atLeastThereIsAnAddressOrAPublicKey = totalOfPayIdAddresses.value ?: 0 > 0 || totalOfPayIdPublicKeys.value ?: 0 > 0
        return _payId.value != null && atLeastThereIsAnAddressOrAPublicKey
    }
}
