package io.iohk.atala.prism.app.ui.payid.step1

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import io.iohk.atala.prism.app.neo.data.PayIdRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class PayIdSelectIdentityCredentialViewModel @Inject constructor(private val repository: PayIdRepository) : ViewModel() {

    private val _identityCredentials = MutableLiveData<List<CheckableData<Credential>>>(listOf())

    val identityCredentials: LiveData<List<CheckableData<Credential>>> = _identityCredentials

    val showNoIdentityCredentialsMessage: LiveData<Boolean> = Transformations.map(identityCredentials) {
        it.isEmpty()
    }

    val selectedIdentityCredentials: LiveData<List<Credential>> = Transformations.map(_identityCredentials) {
        it.filter { item -> item.isChecked }.map { checkable -> checkable.data }
    }

    private val _othersCredentials = MutableLiveData<List<CheckableData<Credential>>>(listOf())

    val othersCredentials: LiveData<List<CheckableData<Credential>>> = _othersCredentials

    val canContinue: LiveData<Boolean> = Transformations.map(selectedIdentityCredentials) { it.isNotEmpty() }

    private val _shouldContinue = MutableLiveData<EventWrapper<List<Credential>>>()

    val shouldContinue: LiveData<EventWrapper<List<Credential>>> = _shouldContinue

    fun loadCredentials() {
        viewModelScope.launch {
            _identityCredentials.value = repository.getIdentityCredentials().map {
                CheckableData(it)
            }
            _othersCredentials.value = repository.getNotIdentityCredentials().map {
                CheckableData(it)
            }
        }
    }

    fun selectCredential(credential: CheckableData<Credential>) {
        credential.setChecked(!credential.isChecked)
        if (_identityCredentials.value?.contains(credential) == true) {
            val updatedList = _identityCredentials.value!!.toList()
            _identityCredentials.value = updatedList
        } else if (_othersCredentials.value?.contains(credential) == true) {
            val updatedList = _othersCredentials.value!!.toList()
            _othersCredentials.value = updatedList
        }
    }

    fun next() {
        if (canContinue.value == true) {
            val othersSelectedCredentials = _othersCredentials.value?.filter { it.isChecked }?.map { it.data }
            val allSelectedCredentials = selectedIdentityCredentials.value!!.toMutableList()
            allSelectedCredentials.addAll(othersSelectedCredentials ?: listOf())
            _shouldContinue.value = EventWrapper(allSelectedCredentials.toList())
        }
    }
}
