package io.iohk.atala.prism.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.DataManager
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.grpc.AsyncTaskResult
import io.iohk.atala.prism.app.grpc.ParticipantInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class NewConnectionsViewModel() : CvpViewModel() {

    private lateinit var appDataManager: DataManager

    private val issuerInfo = MutableLiveData<AsyncTaskResult<ParticipantInfoResponse>>()

    constructor(dataManager: DataManager) : this() {
        appDataManager = dataManager
    }

    fun getConnectionTokenInfoLiveData(): LiveData<AsyncTaskResult<ParticipantInfoResponse>>? {
        return issuerInfo
    }

    fun getConnectionTokenInfo(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val participantInfoResponse = appDataManager.getConnectionTokenInfo(token)
                val connectionsList = appDataManager.getAllContacts()
                val found = connectionsList.any { connection: Contact ->
                    connection.name == participantInfoResponse.creator.issuer.name || connection.name == participantInfoResponse.creator.holder.name
                }
                issuerInfo.postValue(AsyncTaskResult(ParticipantInfoResponse(participantInfoResponse.creator, token, found)))
            } catch (ex: Exception) {
                issuerInfo.postValue(AsyncTaskResult(ex))
            }
        }
    }
}
