package io.iohk.atala.prism.app.ui.payid.addresslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.neo.data.PayIdRepository
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class PayIdAddressListViewModel @Inject constructor(private val repository: PayIdRepository) : ViewModel() {
    private val _payId = MutableLiveData<PayId?>()

    val payIdName: LiveData<String> = Transformations.map(_payId) { payId ->
        payId?.name ?: ""
    }

    val addresses: LiveData<List<PayIdAddress>> = repository.payIdAddresses

    fun loadPayIdData() {
        viewModelScope.launch {
            try {
                _payId.value = repository.loadCurrentPayId()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
