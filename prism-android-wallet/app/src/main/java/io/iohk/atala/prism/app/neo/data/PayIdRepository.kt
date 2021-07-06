package io.iohk.atala.prism.app.neo.data

import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.data.local.PayIdLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.app.utils.CryptoUtils
import kotlinx.coroutines.TimeoutCancellationException
import java.lang.Exception
import java.util.concurrent.ExecutionException
import kotlin.jvm.Throws

class PayIdRepository(
    private val payIdLocalDataSource: PayIdLocalDataSourceInterface,
    private val mirrorMessageSender: MirrorMessageSender,
    private val remoteDataSource: ConnectorRemoteDataSource,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) :
    BaseRepository(
        sessionLocalDataSource,
        preferencesLocalDataSource
    ) {
    suspend fun getIdentityCredentials(): List<Credential> = payIdLocalDataSource.getIdentityCredentials()

    suspend fun getNotIdentityCredentials(): List<Credential> = payIdLocalDataSource.getNotIdentityCredentials()

    @Throws(PayIdRepositoryException::class, ExecutionException::class, InterruptedException::class, TimeoutCancellationException::class)
    suspend fun loadCurrentPayIdName(): String? {
        val mirrorContact = getMirrorContact()
        return mirrorMessageSender.sendGetPayIdNameMessage(mirrorContact)
    }

    @Throws(PayIdRepositoryException::class, ExecutionException::class, InterruptedException::class, TimeoutCancellationException::class)
    suspend fun registerPayIdName(payIdName: String) {
        val mirrorContact = getMirrorContact()
        val payIdNameIsAvailable = mirrorMessageSender.sendCheckPayIdNameAvailabilityMessage(payIdName, mirrorContact) ?: false
        if (!payIdNameIsAvailable) throw PayIdRepositoryException.PayIdNameAlreadyTaken(payIdName)
        val payIdNameRegistrationResult = mirrorMessageSender.sendPayIdNameRegistrationMessage(payIdName, mirrorContact)
        if (!payIdNameRegistrationResult) throw PayIdRepositoryException.PayIdNameAlreadyTaken(payIdName)
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
}
