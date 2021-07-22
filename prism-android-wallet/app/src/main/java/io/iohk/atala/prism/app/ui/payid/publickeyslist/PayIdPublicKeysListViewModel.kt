package io.iohk.atala.prism.app.ui.payid.publickeyslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdPublicKey
import io.iohk.atala.prism.app.neo.data.PayIdRepository
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

class PayIdPublicKeysListViewModel @Inject constructor(private val repository: PayIdRepository) : ViewModel() {
    private val _payId = MutableLiveData<PayId?>()

    val payIdName: LiveData<String> = Transformations.map(_payId) { payId ->
        payId?.name ?: ""
    }

    val publicKeys: LiveData<List<PayIdPublicKey>> = repository.payIdPublicKeys

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
