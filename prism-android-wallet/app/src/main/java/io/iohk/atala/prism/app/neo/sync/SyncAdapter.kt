package io.iohk.atala.prism.app.neo.sync

import android.accounts.Account
import android.content.*
import android.os.Bundle
import android.util.Log
import io.iohk.atala.prism.app.data.local.db.AppDatabase
import io.iohk.atala.prism.app.data.local.db.mappers.CredentialMapper
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.data.local.db.model.ProofRequest
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSource
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSource
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.app.utils.CryptoUtils
import io.iohk.atala.prism.protos.AtalaMessage

class SyncAdapter @JvmOverloads constructor(
        context: Context,
        autoInitialize: Boolean,
        allowParallelSyncs: Boolean = false
) : AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {

    private val logTag = javaClass.name

    private val appDatabase: AppDatabase by lazy {
        AppDatabase.Builder.build(context)
    }

    private val sessionLocalDataSource: SessionLocalDataSource by lazy {
        SessionLocalDataSource(context)
    }

    private val connectorApi: ConnectorRemoteDataSource by lazy {
        ConnectorRemoteDataSource(PreferencesLocalDataSource(context))
    }

    override fun onPerformSync(account: Account?, extras: Bundle?, authority: String?, provider: ContentProviderClient?, syncResult: SyncResult?) {
        sessionLocalDataSource.getSessionData()?.let { mnemonicList ->
            Log.i(logTag, "SYNC CONNECTIONS MESSAGES");
            syncConnectionsMessages(mnemonicList)
            Log.i(logTag, "SYNC FINISHED");
        }
    }

    private fun syncConnectionsMessages(mnemonicList: List<String>) {
        val contacts = appDatabase.contactDao().getAllSync()
        val credentials: MutableList<Credential> = appDatabase.credentialDao().getAllCredentials().toMutableList()
        contacts.forEach { contact ->
            val credentialsToStore: MutableList<Credential> = mutableListOf()
            val proofRequestToStore: MutableList<Pair<ProofRequest, List<Credential>>> = mutableListOf()
            val keyPair = CryptoUtils.getKeyPairFromPath(contact.keyDerivationPath, mnemonicList)
            val messages = connectorApi.getAllMessages(keyPair, contact.lastMessageId).messagesList
            messages.forEach { receivedMessage ->
                val atalaMessage = AtalaMessage.parseFrom(receivedMessage.message)
                if (CredentialMapper.isACredentialMessage(atalaMessage)) {
                    val credential = CredentialMapper.mapToCredential(atalaMessage, receivedMessage.id, receivedMessage.connectionId, receivedMessage.received, contact)
                    credentialsToStore.add(credential)
                    credentials.add(credential)
                } else if (atalaMessage.messageCase == AtalaMessage.MessageCase.PROOFREQUEST) {
                    mapProofRequest(atalaMessage.proofRequest, receivedMessage.id, contact.connectionId, credentials)?.let {
                        proofRequestToStore.add(it)
                    }
                }
                contact.lastMessageId = receivedMessage.id
            }
            appDatabase.contactDao().updateContactSync(contact, credentialsToStore)
            appDatabase.proofRequestDao().insertAllSync(proofRequestToStore)
        }
    }

    private fun mapProofRequest(proofRequestMessage: io.iohk.atala.prism.protos.ProofRequest, messageId: String, connectionId: String, currentCredentials: List<Credential>): Pair<ProofRequest, List<Credential>>? {
        val credentialsFound: List<Credential> = proofRequestMessage.typeIdsList.map { typeId ->
            currentCredentials.find { credential -> credential.credentialType == typeId }
        }.filterNotNull()
        // if not all requested credentials are found, the proof request is dismissed
        if (credentialsFound.size == proofRequestMessage.typeIdsList.size) {
            return Pair(
                    ProofRequest(connectionId, messageId),
                    credentialsFound
            )
        }
        return null
    }
}