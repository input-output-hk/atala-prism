package io.iohk.atala.prism.app.neo.data

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.crypto.derivation.MnemonicLengthException
import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.app.data.local.db.mappers.CredentialMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.neo.common.exceptions.InvalidSecurityWord
import io.iohk.atala.prism.app.neo.common.exceptions.InvalidSecurityWordsLength
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.protos.AtalaMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException

class AccountRecoveryRepository(private val sessionLocalDataSource: SessionLocalDataSourceInterface,
                                private val contactsLocalDataSource: ContactsLocalDataSourceInterface,
                                private val connectorApi: ConnectorRemoteDataSource) {

    suspend fun recoverAccount(words: List<String>) {
        return withContext(Dispatchers.IO) {
            // check if is a valid MnemonicList
            if (!isAValidMnemonicList(words)) {
                return@withContext
            }
            // load all data from Server
            var currentIndex = 0
            var lastIndex = 0
            var done = false
            val loadedContactsAndCredentials = mutableMapOf<Contact, List<Credential>>()
            while (!done) {
                try {
                    val contacts = loadContactsAt(currentIndex, words)
                    contacts.forEach { (contact, ecKeyPair) ->
                        val credentials = loadCredentialsFromContact(contact, ecKeyPair)
                        loadedContactsAndCredentials[contact] = credentials
                    }
                } catch (ex: ExecutionException) {
                    val statusRuntimeException = ex.cause as? StatusRuntimeException ?: throw ex
                    if (statusRuntimeException.status.code == Status.UNKNOWN.code) {
                        done = true
                        lastIndex = currentIndex - 1
                    } else {
                        throw statusRuntimeException
                    }
                }
                currentIndex += 1
            }
            // Store all data in local
            sessionLocalDataSource.storeSessionData(words)
            contactsLocalDataSource.storeContactsWithIssuedCredentials(loadedContactsAndCredentials)
            sessionLocalDataSource.storeLastSyncedIndex(lastIndex)
        }
    }

    /*
    * Verify if they are valid security words
    * */
    private suspend fun isAValidMnemonicList(words: List<String>): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                return@withContext CryptoUtils.isValidMnemonicList(words)
            } catch (exception: MnemonicLengthException) {
                throw InvalidSecurityWordsLength("Wrong number of words, must be exactly 12")
            } catch (_: Exception) {
                throw InvalidSecurityWord("One or more words in the list are not valid")
            }
        }
    }


    /*
    * For now the application is in the middle of a refactoring process, it is likely that the
    * following functions (loadContactsAt,loadCredentialsFromContact) will become part of the
    * "remoteDataSource" layer so that they can be reused in different repositories.
    * */

    private fun loadContactsAt(index: Int, mnemonicList: List<String>): Map<Contact, ECKeyPair> {
        val map = mutableMapOf<Contact, ECKeyPair>()
        val path = CryptoUtils.getPathFromIndex(index)
        val ecKeyPair = CryptoUtils.getKeyPairFromPath(path, mnemonicList)
        val paginatedConnections = connectorApi.getConnections(ecKeyPair)
        paginatedConnections.connectionsList.forEach {
            val contact = ContactMapper.mapToContact(it, path)
            map[contact] = ecKeyPair
        }
        return map
    }

    private fun loadCredentialsFromContact(contact: Contact, keyPair: ECKeyPair): List<Credential> {
        val paginatedMessagesResponse = connectorApi.getAllMessages(keyPair, contact.lastMessageId)
        return paginatedMessagesResponse.messagesList.map { receivedMessage ->
            val atalaMessage = AtalaMessage.parseFrom(receivedMessage.message)
            contact.lastMessageId = receivedMessage.id
            return@map if (CredentialMapper.isACredentialMessage(atalaMessage))
                CredentialMapper.mapToCredential(atalaMessage, receivedMessage.id, receivedMessage.connectionId, receivedMessage.received, contact)
            else
                null
        }.filterNotNull()
    }
}