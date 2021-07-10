package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.iohk.atala.prism.app.core.ConnectorListenerService
import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.KycRequest
import io.iohk.atala.prism.app.neo.common.extensions.getOrAwaitValue
import io.iohk.atala.prism.app.neo.data.local.KycLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.protos.AtalaMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * This is a provisional method to initialize the Acuant Flow.
 * Basically, to start this process, first you have to generate a connection token in
 * KycBridgeService/CreateAccount, then with that token add a new connection through
 * the connector (AddConnectionFromToken), after that it would have to receive an
 * [AtalaMessage.MessageCase.KYCBRIDGEMESSAGE] message from that new connection
 * through the connector with the necessary data (bearer_token and instance_id) to start the verification process with Acuant.
 * */
class KycInitializationHelper(
    private val kycLocalDataSource: KycLocalDataSourceInterface,
    private val remoteDataSource: ConnectorRemoteDataSource,
    private val sessionLocalDataSource: SessionLocalDataSourceInterface,
) {

    sealed class KycInitializationResult {
        class Error(val ex: Exception) : KycInitializationResult()
        class Success(val kycRequest: KycRequest) : KycInitializationResult()
        object TimeoutError : KycInitializationResult()
        object IsLoaDing : KycInitializationResult()
    }

    companion object {
        /*
        * Once the connection with the kyc bridge is created, the application will wait for
        * MAX_WAITING_TIME_MILL to receive the [AtalaMessage.MessageCase.KYCBRIDGEMESSAGE]
        * */
        private const val MAX_WAITING_TIME_MILL = 30000L
    }

    // mnemonic list
    private lateinit var sessionData: List<String>

    // this LiveData holds the state of the initialization process
    private val result = MutableLiveData<KycInitializationResult?>()

    val status: LiveData<KycInitializationResult?> = result

    private var kycConnectionContact: Contact? = null

    /**
     * Is the starting point of the Acuant verification process
     * */
    fun initializeAcuantProcess(): LiveData<KycInitializationResult?> {
        if (result.value == KycInitializationResult.IsLoaDing) { return result }
        result.value = KycInitializationResult.IsLoaDing
        CoroutineScope(Dispatchers.Default).launch {
            // obtain session data (mnemonic list)
            sessionData = sessionLocalDataSource.getSessionData()!!
            kycLocalDataSource.kycContact()?.let {
                kycLocalDataSource.kycRequestSync()?.let {
                    // When there is data stored for a [AtalaMessage.MessageCase.KYCBRIDGEMESSAGE] message it means that Acuant initialization is already success
                    result.postValue(KycInitializationResult.Success(it))
                } ?: kotlin.run {
                    // When there is already a connection with the KYC bridge but there is no local data for a [AtalaMessage.MessageCase.KYCBRIDGEMESSAGE] message, we should go to step 3
                    kycConnectionContact = it
                    waitForKycRequest() // Go to Step3
                }
            } ?: kotlin.run {
                // When there is not at least one connection with the KYC bridge we must start the connection (step 1)
                getConnectionToken()
            }
        }
        return result
    }

    /**
     * Acuant flow initialization Step1: obtain a connection token from KycBridgeService/CreateAccount
     * */
    private suspend fun getConnectionToken() {
        try {
            // get a connection token
            val response = remoteDataSource.kycBridgeCreateAccount()
            val token = response.connectionToken
            // Go to Step2
            acceptKycConnection(token)
        } catch (ex: Exception) {
            ex.printStackTrace()
            result.postValue(KycInitializationResult.Error(ex))
        }
    }

    /**
     * Acuant flow initialization Step2: Accept a connection obtained in step 1
     * */
    private suspend fun acceptKycConnection(token: String) {
        val currentIndex = sessionLocalDataSource.getLastSyncedIndex()
        val newPath = CryptoUtils.getNextPathFromIndex(currentIndex)
        val mnemonicList = sessionLocalDataSource.getSessionData()!!
        val keypair = CryptoUtils.getKeyPairFromPath(newPath, mnemonicList)
        val addConnectionResponse = remoteDataSource.addConnection(keypair, token)
        val contact = ContactMapper.mapToContact(addConnectionResponse.connection, newPath)
        // When saving this new connection/contact, the application will receive a message of type
        // [AtalaMessage.MessageCase.KYCBRIDGEMESSAGE] through the connector, with the
        // necessary data to start the verification process with Acuant.
        kycLocalDataSource.storeKycContact(contact)
        sessionLocalDataSource.increaseSyncedIndex()
        kycConnectionContact = contact
        waitForKycRequest() // Go To Step3
    }

    /**
     * Acuant flow initialization Step3: At this point, the KYC connection should already be established.
     * then it is necessary to wait for the [AtalaMessage.MessageCase.KYCBRIDGEMESSAGE] message through
     * the connector (the reception of messages from the connector is managed entirely by [ConnectorListenerService])
     * */
    private suspend fun waitForKycRequest() {
        // it is necessary to run this code in the main thread to be able to observe when it exists a [KycRequest] in the local database
        GlobalScope.launch(Dispatchers.Main) {
            kycLocalDataSource.kycRequestAsync().getOrAwaitValue(MAX_WAITING_TIME_MILL)?.let {
                // when the [AtalaMessage.MessageCase.KYCBRIDGEMESSAGE] message data is already stored locally the initialization process is complete
                result.postValue(KycInitializationResult.Success(it))
            } ?: kotlin.run {
                result.postValue(KycInitializationResult.TimeoutError)
            }
        }
    }
}
