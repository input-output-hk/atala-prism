package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.data.local.db.model.PayIdPublicKey
import io.iohk.atala.prism.app.neo.common.extensions.getOrAwaitValue
import io.iohk.atala.prism.app.neo.common.softCardanoAddressValidation
import io.iohk.atala.prism.app.neo.common.softCardanoExtendedPublicKeyValidation
import io.iohk.atala.prism.app.neo.data.local.PayIdLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.protos.AtalaMessage
import io.iohk.atala.prism.protos.MirrorMessage
import kotlinx.coroutines.TimeoutCancellationException
import java.lang.Exception
import java.util.concurrent.ExecutionException
import kotlin.jvm.Throws

class PayIdRepository(
    private val payIdLocalDataSource: PayIdLocalDataSourceInterface,
    private val mirrorMessageResponseHandler: MirrorMessageResponseHandler,
    private val remoteDataSource: ConnectorRemoteDataSource,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) :
    BaseRepository(
        sessionLocalDataSource,
        preferencesLocalDataSource
    ) {

    companion object {
        const val DEFAULT_MAX_TIMEOUT_MILL = 20000L
    }

    val totalOfPayIdAddresses: LiveData<Int> = payIdLocalDataSource.totalOfPayIdAddresses()

    val totalOfPayIdPublicKeys: LiveData<Int> = payIdLocalDataSource.totalOfPayIdPublicKeys()

    val payIdAddresses: LiveData<List<PayIdAddress>> = payIdLocalDataSource.registeredPayIdAddresses()

    val payIdPublicKeys: LiveData<List<PayIdPublicKey>> = payIdLocalDataSource.registeredPayIdPublicKeys()

    suspend fun getIdentityCredentials(): List<Credential> = payIdLocalDataSource.getIdentityCredentials()

    suspend fun getNotIdentityCredentials(): List<Credential> = payIdLocalDataSource.getNotIdentityCredentials()

    @Throws(TimeoutCancellationException::class)
    suspend fun loadCurrentPayId(): PayId? {
        return if (payIdLocalDataSource.getPayIdByStatus(PayId.Status.WaitingForResponse) != null) {
            // if there is already a PayId waiting to be answered, it waits for the answer
            payIdLocalDataSource.getPayIdByStatusLiveData(PayId.Status.Registered).getOrAwaitValue(DEFAULT_MAX_TIMEOUT_MILL)
        } else payIdLocalDataSource.getPayIdByStatus(PayId.Status.Registered)
    }

    @Throws(
        PayIdRepositoryException.AtalaError::class,
        PayIdRepositoryException.PayIdAlreadyRegistered::class,
        ExecutionException::class,
        InterruptedException::class,
        TimeoutCancellationException::class
    )
    suspend fun checkPayIdNameAvailability(payIdName: String): Boolean {
        loadCurrentPayId()?.let {
            throw PayIdRepositoryException.PayIdAlreadyRegistered(it.name ?: "")
        }
        val mirrorContact = getMirrorContact()
        val payIdNameIsAvailableMessageId = remoteDataSource.sendCheckPayIdNameAvailabilityMessage(payIdName, mirrorContact)
        return mirrorMessageResponseHandler.awaitForResponse(payIdNameIsAvailableMessageId)?.let { atalaMessage ->
            if (atalaMessage.messageCase == AtalaMessage.MessageCase.ATALA_ERROR_MESSAGE) {
                throw PayIdRepositoryException.AtalaError(atalaMessage.atalaErrorMessage.status.message)
            }
            atalaMessage.mirrorMessage?.checkPayIdNameAvailabilityResponse?.available == true
        }
    }

    @Throws(
        PayIdRepositoryException.AtalaError::class,
        PayIdRepositoryException.PayIdNameAlreadyTaken::class,
        ExecutionException::class,
        InterruptedException::class,
        TimeoutCancellationException::class
    )
    suspend fun registerPayIdName(payIdName: String): PayId? {
        loadCurrentPayId()?.let { return@registerPayIdName it }
        val mirrorContact = getMirrorContact()
        val payIdNameRegistrationMessageId = remoteDataSource.sendPayIdNameRegistrationMessage(payIdName, mirrorContact)
        // PayID registration request is saved locally waiting to be answered (the response will be handled in ConnectorListenerRepository)
        payIdLocalDataSource.storePayId(PayId(mirrorContact.connectionId, payIdName, payIdNameRegistrationMessageId, PayId.Status.WaitingForResponse))
        return mirrorMessageResponseHandler.awaitForResponse(payIdNameRegistrationMessageId).let { atalaMessage ->
            when {
                atalaMessage.mirrorMessage?.messageCase == MirrorMessage.MessageCase.PAYID_NAME_REGISTERED_MESSAGE ->
                    payIdLocalDataSource
                        .getPayIdByStatusLiveData(PayId.Status.Registered)
                        .getOrAwaitValue(DEFAULT_MAX_TIMEOUT_MILL)
                atalaMessage.mirrorMessage?.messageCase == MirrorMessage.MessageCase.PAYID_NAME_TAKEN_MESSAGE ->
                    throw PayIdRepositoryException.PayIdNameAlreadyTaken(payIdName)
                atalaMessage.messageCase == AtalaMessage.MessageCase.ATALA_ERROR_MESSAGE ->
                    throw PayIdRepositoryException.AtalaError(atalaMessage.atalaErrorMessage.status.message)
                else -> throw Exception("unexpected reply message for a PayIdNameRegistrationMessage")
            }
        }
    }

    suspend fun registerAddressOrPublicKey(addressOrPublicKey: String) {
        payIdLocalDataSource.getPayIdByStatus(PayId.Status.Registered)?.let { payId ->
            when {
                softCardanoAddressValidation(addressOrPublicKey) -> {
                    registerCardanoAddress(addressOrPublicKey, payId)
                }
                softCardanoExtendedPublicKeyValidation(addressOrPublicKey) -> {
                    registerPublicKey(addressOrPublicKey, payId)
                }
                else -> throw Exception("Invalid Address Format")
            }
        } ?: throw Exception("There is no PayId")
    }
    @Throws(PayIdRepositoryException::class, ExecutionException::class, InterruptedException::class, TimeoutCancellationException::class)
    private suspend fun registerCardanoAddress(cardanoAddress: String, payId: PayId) {
        val mirrorContact = getMirrorContact()
        val messageId = remoteDataSource.sendRegisterAddressMessage(cardanoAddress, mirrorContact)
        mirrorMessageResponseHandler.awaitForResponse(messageId, DEFAULT_MAX_TIMEOUT_MILL)?.let { atalaMessage ->
            if (atalaMessage.messageCase == AtalaMessage.MessageCase.ATALA_ERROR_MESSAGE) {
                throw PayIdRepositoryException.AtalaError(atalaMessage.atalaErrorMessage.status.message)
            } else if (atalaMessage.mirrorMessage?.messageCase != MirrorMessage.MessageCase.ADDRESS_REGISTERED_MESSAGE)
                throw PayIdRepositoryException.AtalaError("Unknown API Error")
        }
    }

    @Throws(PayIdRepositoryException::class, ExecutionException::class, InterruptedException::class, TimeoutCancellationException::class)
    suspend fun registerPublicKey(publicKey: String, payId: PayId) {
        val mirrorContact = getMirrorContact()
        val messageId = remoteDataSource.sendRegisterWalletMessage(publicKey, mirrorContact)
        mirrorMessageResponseHandler.awaitForResponse(messageId, DEFAULT_MAX_TIMEOUT_MILL)?.let { atalaMessage ->
            if (atalaMessage.messageCase == AtalaMessage.MessageCase.ATALA_ERROR_MESSAGE) {
                throw PayIdRepositoryException.AtalaError(atalaMessage.atalaErrorMessage.status.message)
            } else if (atalaMessage.mirrorMessage?.messageCase != MirrorMessage.MessageCase.WALLET_REGISTERED)
                throw PayIdRepositoryException.AtalaError("Unknown API Error")
        }
    }

    private suspend fun getMirrorContact(): Contact {
        return payIdLocalDataSource.getCurrentPayIdContact() ?: createPayIdConnection()
    }

    private suspend fun createPayIdConnection(): Contact {
        val connectionToken = remoteDataSource.mirrorServiceCreateAccount().connectionToken
        val currentIndex = sessionLocalDataSource.getLastSyncedIndex()
        val newPath = CryptoUtils.getNextPathFromIndex(currentIndex)
        val mnemonicList = sessionLocalDataSource.getSessionData()!!
        val keypair = CryptoUtils.getKeyPairFromPath(newPath, mnemonicList)
        val response = remoteDataSource.addConnection(keypair, connectionToken)
        val contact = ContactMapper.mapToContact(response.connection, newPath)
        payIdLocalDataSource.storePayIdContact(contact)
        sessionLocalDataSource.increaseSyncedIndex()
        return contact
    }
}

sealed class PayIdRepositoryException(message: String?) : Exception(message) {
    class PayIdNameAlreadyTaken(val payIdName: String) : PayIdRepositoryException("'$payIdName' is already taken")
    class PayIdAlreadyRegistered(val payIdName: String) : PayIdRepositoryException("'$payIdName' is already registered")
    class AtalaError(message: String?) : PayIdRepositoryException(message)
}
