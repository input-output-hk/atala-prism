package io.iohk.atala.prism.app.ui.payid.addaddressform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.softCardanoAddressValidation
import io.iohk.atala.prism.app.neo.common.softCardanoExtendedPublicKeyValidation
import io.iohk.atala.prism.app.neo.data.PayIdRepository
import io.iohk.atala.prism.app.neo.data.PayIdRepositoryException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class AddAddressFormViewModel @Inject constructor(private val repository: PayIdRepository) : ViewModel() {

    sealed class Error {
        object TimeOutError : Error()
        class ServerError(val message: String?) : Error()
        class UnknownError(val message: String?) : Error()
    }

    val walletPublicKey = MutableLiveData("")

    val isAValidAddress: LiveData<Boolean> = Transformations.map(walletPublicKey) {
        softCardanoExtendedPublicKeyValidation(it) || softCardanoAddressValidation(it)
    }

    private val _isLoading = MutableLiveData(false)

    val isLoading: LiveData<Boolean> = _isLoading

    private val _successEvent = MutableLiveData<EventWrapper<Boolean>>()

    val successEvent: LiveData<EventWrapper<Boolean>> = _successEvent

    private val _error = MutableLiveData<EventWrapper<Error>>()

    val error: LiveData<EventWrapper<Error>> = _error

    fun addAddress() {
        walletPublicKey.value?.let { walletPublicKey ->
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    repository.registerCardanoAddress(walletPublicKey)
                    _isLoading.value = false
                    _successEvent.value = EventWrapper(true)
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
}
