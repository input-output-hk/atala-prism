package io.iohk.cvp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.iohk.cvp.data.DataManager
import io.iohk.cvp.data.local.db.mappers.ContactMapper
import io.iohk.cvp.grpc.AsyncTaskResult
import io.iohk.prism.protos.AddConnectionFromTokenResponse
import io.iohk.prism.protos.ConnectorPublicKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(private val dataManager: DataManager) : NewConnectionsViewModel(dataManager) {

    private val _newConnectionInfoLiveData = MutableLiveData<AsyncTaskResult<AddConnectionFromTokenResponse>>()
    private val _hasConnectionsInitialScreenLiveData = MutableLiveData<AsyncTaskResult<Boolean>>()
    private val _hasConnectionsMoveToContact = MutableLiveData<AsyncTaskResult<Boolean>>()

    fun addConnectionFromToken(token: String, publicKey: ConnectorPublicKey, nonce: String)
            : LiveData<AsyncTaskResult<AddConnectionFromTokenResponse>>? {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val addConnectionFromTokenResponse = dataManager.addConnection(token, publicKey, nonce)
                dataManager.saveContact(ContactMapper.mapToContact(addConnectionFromTokenResponse))
                _newConnectionInfoLiveData.postValue(AsyncTaskResult(addConnectionFromTokenResponse))
            } catch (ex:Exception) {
                _newConnectionInfoLiveData.postValue(AsyncTaskResult(ex))
            }
        }
        return _newConnectionInfoLiveData
    }

    fun checkIfHasConnectionsInitialScreen() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _hasConnectionsInitialScreenLiveData.postValue(getConnections())
            } catch (ex:Exception) {
                _hasConnectionsInitialScreenLiveData.postValue(AsyncTaskResult(ex))
            }
        }
    }

    fun checkIfHasConnectionsMoveToContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _hasConnectionsMoveToContact.postValue(getConnections())
            } catch (ex:Exception) {
                _hasConnectionsMoveToContact.postValue(AsyncTaskResult(ex))
            }
        }
    }

    private suspend fun getConnections(): AsyncTaskResult<Boolean> {
        val connectionsList = dataManager.getAllContacts()
        return AsyncTaskResult(connectionsList.isEmpty())
    }

    fun getHasConnectionsInitialScreenLiveData(): LiveData<AsyncTaskResult<Boolean>> {
        return _hasConnectionsInitialScreenLiveData
    }

    fun getHasConnectionsMoveToContactLiveData(): LiveData<AsyncTaskResult<Boolean>> {
        return _hasConnectionsMoveToContact
    }

}