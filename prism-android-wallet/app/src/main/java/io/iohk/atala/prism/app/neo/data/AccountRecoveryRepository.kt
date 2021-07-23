package io.iohk.atala.prism.app.neo.data

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.app.data.local.db.mappers.CredentialMapper
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.CredentialWithEncodedCredential
import io.iohk.atala.prism.app.data.local.db.model.PayId
import io.iohk.atala.prism.app.data.local.db.model.PayIdAddress
import io.iohk.atala.prism.app.data.local.db.model.PayIdPublicKey
import io.iohk.atala.prism.app.neo.common.exceptions.InvalidSecurityWord
import io.iohk.atala.prism.app.neo.common.exceptions.InvalidSecurityWordsLength
import io.iohk.atala.prism.app.neo.common.extensions.toMilliseconds
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PayIdLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.kotlin.crypto.derivation.MnemonicLengthException
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.protos.AtalaMessage
import io.iohk.atala.prism.protos.MirrorMessage
import io.iohk.atala.prism.protos.ReceivedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException

class AccountRecoveryRepository(
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface,
    private val payIdLocalDataSource: PayIdLocalDataSourceInterface,
    private val contactsLocalDataSource: ContactsLocalDataSourceInterface,
    private val connectorApi: ConnectorRemoteDataSource
) : BaseRepository(sessionLocalDataSource, preferencesLocalDataSource) {

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
            val loadedContactsAndCredentials = mutableMapOf<Contact, List<CredentialWithEncodedCredential>>()
            var payId: PayId? = null
            val payIdAddressesMessages = mutableListOf<AtalaMessage>()
            val payIdPublicKeysMessages = mutableListOf<AtalaMessage>()
            while (!done) {
                try {
                    val contacts = loadContactsAt(currentIndex, words)
                    contacts.forEach { (contact, ecKeyPair) ->
                        val credentials: MutableList<CredentialWithEncodedCredential> = mutableListOf()
                        cycleThroughMessages(contact, ecKeyPair) { atalaMessage, receivedMessage ->
                            when {
                                // Credential Recovery
                                atalaMessage.messageCase == AtalaMessage.MessageCase.PLAIN_CREDENTIAL -> {
                                    val credential = CredentialMapper.mapToCredential(receivedMessage, receivedMessage.id, receivedMessage.connectionId, receivedMessage.received.toMilliseconds(), contact)
                                    credentials.add(credential)
                                }
                                // Pay Id Name Recovery
                                atalaMessage.mirrorMessage?.messageCase == MirrorMessage.MessageCase.PAYID_NAME_REGISTERED_MESSAGE -> {
                                    payId = PayId(
                                        connectionId = contact.connectionId,
                                        name = atalaMessage.mirrorMessage!!.payIdNameRegisteredMessage.name,
                                        messageId = receivedMessage.id,
                                        status = PayId.Status.Registered
                                    )
                                }
                                // Pay Id Address Recovery
                                atalaMessage.mirrorMessage?.messageCase == MirrorMessage.MessageCase.ADDRESS_REGISTERED_MESSAGE -> {
                                    payIdAddressesMessages.add(atalaMessage)
                                }
                                // Pay Id Public Key Recovery
                                atalaMessage.mirrorMessage?.messageCase == MirrorMessage.MessageCase.WALLET_REGISTERED -> {
                                    payIdPublicKeysMessages.add(atalaMessage)
                                }
                            }
                        }
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

            // Store Pay Id Data
            payId?.let {
                payIdLocalDataSource.setContactAsAPayIdContact(connectionId = it.connectionId)
                val payIdLocalId = payIdLocalDataSource.storePayId(it)
                payIdAddressesMessages.forEach { atalaMessage ->
                    payIdLocalDataSource.createPayIdAddress(
                        PayIdAddress(
                            payIdLocalId = payIdLocalId,
                            address = atalaMessage.mirrorMessage.addressRegisteredMessage.cardanoAddress,
                            messageId = atalaMessage.replyTo
                        )
                    )
                }
                payIdPublicKeysMessages.forEach { atalaMessage ->
                    payIdLocalDataSource.createPayIdPublicKey(
                        PayIdPublicKey(
                            payIdLocalId = payIdLocalId,
                            publicKey = atalaMessage.mirrorMessage.walletRegistered.extendedPublicKey,
                            messageId = atalaMessage.replyTo
                        )
                    )
                }
            }
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

    private fun cycleThroughMessages(contact: Contact, keyPair: ECKeyPair, operation: (AtalaMessage, ReceivedMessage) -> Unit) {
        val paginatedMessagesResponse = connectorApi.getAllMessages(keyPair, contact.lastMessageId)
        paginatedMessagesResponse.messagesList.map { receivedMessage ->
            contact.lastMessageId = receivedMessage.id
            AtalaMessage.parseFrom(receivedMessage.message)?.apply {
                operation(this, receivedMessage)
            }
        }
    }
}
