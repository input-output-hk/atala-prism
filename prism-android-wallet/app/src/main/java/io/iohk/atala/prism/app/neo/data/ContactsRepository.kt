package io.iohk.atala.prism.app.neo.data

import androidx.lifecycle.LiveData
import io.iohk.atala.prism.app.data.local.db.mappers.ContactMapper
import io.iohk.atala.prism.app.data.local.db.model.ActivityHistoryWithCredential
import io.iohk.atala.prism.app.data.local.db.model.Contact
import io.iohk.atala.prism.app.data.local.db.model.Credential
import io.iohk.atala.prism.app.grpc.ParticipantInfoResponse
import io.iohk.atala.prism.app.neo.data.local.ContactsLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.PreferencesLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.local.SessionLocalDataSourceInterface
import io.iohk.atala.prism.app.neo.data.remote.ConnectorRemoteDataSource
import io.iohk.atala.prism.app.utils.CryptoUtils

class ContactsRepository(
    private val contactsLocalDataSource: ContactsLocalDataSourceInterface,
    private val remoteDataSource: ConnectorRemoteDataSource,
    sessionLocalDataSource: SessionLocalDataSourceInterface,
    preferencesLocalDataSource: PreferencesLocalDataSourceInterface
) :
    BaseRepository(
        sessionLocalDataSource,
        preferencesLocalDataSource
    ) {

    fun allContacts(): LiveData<List<Contact>> = contactsLocalDataSource.allContacts()

    suspend fun getContactById(contactId: Int): Contact? = contactsLocalDataSource.getContactById(contactId)

    suspend fun getCredentialsActivityHistories(connectionId: String): List<ActivityHistoryWithCredential> = contactsLocalDataSource.getCredentialsActivityHistories(connectionId)

    suspend fun getIssuedCredentials(contactConnectionId: String): List<Credential> = contactsLocalDataSource.getIssuedCredentials(contactConnectionId)

    suspend fun deleteContact(contact: Contact) = contactsLocalDataSource.deleteContact(contact)

    suspend fun getParticipantInfoResponse(token: String): ParticipantInfoResponse {
        val connectionInfo = remoteDataSource.getConnectionTokenInfo(token)
        val allContacts = contactsLocalDataSource.getAllContacts()
        val found = allContacts.any { contact: Contact ->
            contact.did == connectionInfo.creator.holder.did || contact.did == connectionInfo.creator.issuer.did
        }
        return ParticipantInfoResponse(connectionInfo.creator, token, found)
    }

    suspend fun acceptConnection(token: String) {
        val currentIndex = sessionLocalDataSource.getLastSyncedIndex()
        val newPath = CryptoUtils.getNextPathFromIndex(currentIndex)
        val mnemonicList = sessionLocalDataSource.getSessionData()!!
        val keypair = CryptoUtils.getKeyPairFromPath(newPath, mnemonicList)
        val response = remoteDataSource.addConnection(keypair, token, "")
        val contact = ContactMapper.mapToContact(response.connection, newPath)
        contactsLocalDataSource.storeContact(contact)
        sessionLocalDataSource.increaseSyncedIndex()
    }
}
