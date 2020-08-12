package io.iohk.cvp.data.local

import com.google.protobuf.ByteString
import io.iohk.cvp.data.DataManager
import io.iohk.cvp.data.local.db.DbHelper
import io.iohk.cvp.data.local.db.model.Contact
import io.iohk.cvp.data.local.db.model.Credential
import io.iohk.cvp.data.local.remote.ApiHelper
import io.iohk.cvp.viewmodel.dtos.ConnectionListable
import io.iohk.prism.protos.*
import javax.inject.Inject

class AppDataManager @Inject constructor(dbHelper: DbHelper, private var apiHelper: ApiHelper) : DataManager {

    private var mDbHelper: DbHelper = dbHelper

    override suspend fun saveContact(contact: Contact): Long {
        return mDbHelper.saveContact(contact)
    }

    override suspend fun getAllContacts(): List<Contact> {
        return mDbHelper.getAllContacts()
    }

    override suspend fun saveAllCredentials(credentialsList: List<Credential>) {
        mDbHelper.saveAllCredentials(credentialsList)
    }

    override suspend fun addConnection(token: String, publicKey: ConnectorPublicKey, nonce: String): AddConnectionFromTokenResponse {
        return apiHelper.addConnection(token, publicKey, nonce)
    }

    override suspend fun getAllMessages(userId: String, lastMessageId: String?): GetMessagesPaginatedResponse {
        return apiHelper.getAllMessages(userId, lastMessageId)
    }

    override suspend fun sendMultipleMessage(senderUserId: String, connectionId: String, messages: List<ByteString>) {
        return apiHelper.sendMultipleMessage(senderUserId, connectionId, messages)
    }

    override suspend fun getConnectionTokenInfo(token: String): GetConnectionTokenInfoResponse {
        return apiHelper.getConnectionTokenInfo(token)
    }

    override suspend fun sendMessageToMultipleConnections(senderUserList: MutableSet<ConnectionListable>, credential: ByteString) {
        apiHelper.sendMessageToMultipleConnections(senderUserList, credential)
    }

    override suspend fun updateContact(contact: Contact) {
        mDbHelper.updateContact(contact)
    }

    override suspend fun getAllCredentials(): List<Credential> {
        return mDbHelper.getAllCredentials()
    }

    override suspend fun getContactByConnectionId(connectionId: String): Contact? {
        return mDbHelper.getContactByConnectionId(connectionId)
    }

    override suspend fun removeAllLocalData() {
        mDbHelper.removeAllLocalData()
    }

    override suspend fun getAllNewCredentials(): List<Credential> {
        return  mDbHelper.getAllNewCredentials()
    }

    override suspend fun updateCredential(credential: Credential) {
        mDbHelper.updateCredential(credential)
    }

    override suspend fun getCredentialByCredentialId(credentialId: String): Credential? {
        return mDbHelper.getCredentialByCredentialId(credentialId)
    }
}
