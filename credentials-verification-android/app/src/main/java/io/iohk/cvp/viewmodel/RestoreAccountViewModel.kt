package io.iohk.cvp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.iohk.atala.crypto.japi.ECKeyPair
import io.iohk.cvp.R
import io.iohk.cvp.core.CvpApplication
import io.iohk.cvp.data.DataManager
import io.iohk.cvp.data.local.db.mappers.ContactMapper
import io.iohk.cvp.data.local.db.mappers.CredentialMapper
import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.utils.CryptoUtils
import io.iohk.cvp.views.utils.SingleLiveEvent
import io.iohk.prism.protos.AtalaMessage
import io.iohk.prism.protos.ConnectionInfo
import io.iohk.prism.protos.ReceivedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import javax.inject.Inject

class RestoreAccountViewModel @Inject constructor(val dataManager: DataManager, val context: CvpApplication) : ViewModel() {

    companion object {
        private const val STARTING_INDEX: Int = -1
    }

    private val _showErrorMessageLiveData = SingleLiveEvent<String>()

    private val _recoveryCompletedLiveData = MutableLiveData<Boolean>(false)

    fun getAccountRecoveredMessageLiveData(): LiveData<Boolean> {
        return _recoveryCompletedLiveData
    }

    fun getErrorMessageLiveData(): LiveData<String> {
        return _showErrorMessageLiveData
    }

    private fun recoverAccount(phrasesList: MutableList<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lastIndex = recoverConnectionFromIndex(phrasesList, STARTING_INDEX)
                dataManager.saveMnemonics(phrasesList)
                dataManager.saveIndex(lastIndex)
                _recoveryCompletedLiveData.postValue(true)
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                _showErrorMessageLiveData.postValue(context.getString(R.string.server_error_message))
            }
        }
    }

    private suspend fun recoverConnectionFromIndex(mnemonicList: List<String>, currentIndex: Int): Int {
        val nextIndex = currentIndex + 1
        return try {
            val ecKeyPair = CryptoUtils.getKeyPairFromPath("m/$nextIndex'/0'/0'", mnemonicList)
            val getConnectionPaginatedResponse = dataManager.getConnection(ecKeyPair)
            saveAllConcactsAndCredentials(getConnectionPaginatedResponse.connectionsList, CryptoUtils.getPathFromIndex(nextIndex), ecKeyPair)
            recoverConnectionFromIndex(mnemonicList, nextIndex)
        } catch (ex: StatusRuntimeException) {
            if (ex.status.code == Status.UNKNOWN.code) {
                return currentIndex
            }
            throw ex
        }
    }

    private suspend fun saveAllConcactsAndCredentials(connectionsList: List<ConnectionInfo>, keyPath: String, ecKeyPair: ECKeyPair) {
        connectionsList.forEach { connection ->
            val contact = ContactMapper.mapToContact(connection, keyPath)
            getAllCredentialsFromContact(contact, ecKeyPair)
        }
    }

    private suspend fun getAllCredentialsFromContact(contact: Contact, ecKeyPair: ECKeyPair) {
        val messagesList = dataManager.getAllMessages(ecKeyPair, contact.lastMessageId).messagesList
        val credentialList = messagesList.asFlow()
                .filter {
                    val newMessage: AtalaMessage = AtalaMessage.parseFrom(it.message)
                    newMessage.proofRequest.typeIdsList.isEmpty()
                }.toList()

        if (credentialList.isNotEmpty()) {
            contact.lastMessageId = credentialList.last().id
            saveCredentials(credentialList)
        }
        dataManager.saveContact(contact)
    }

    private suspend fun saveCredentials(messagesPaginatedResponseList: List<ReceivedMessage>) {
        val credentialsList = messagesPaginatedResponseList.map { receivedMessage: ReceivedMessage? ->
            return@map CredentialMapper.mapToCredential(receivedMessage)
        }.toList()
        dataManager.saveAllCredentials(credentialsList)
    }


    fun validateMnemonics(phrasesList: MutableList<String>) {
        try {
            if (phrasesList.size < 12) {
                _showErrorMessageLiveData.postValue(context.getString(R.string.recovery_must_have_twelve_words))
            } else if (!CryptoUtils.isValidMnemonicList(phrasesList)) {
                _showErrorMessageLiveData.postValue(context.getString(R.string.incorrect_recovery_phrase))
            } else {
                recoverAccount(phrasesList)
            }
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            _showErrorMessageLiveData.postValue(context.getString(R.string.incorrect_recovery_phrase))
        }
    }

}